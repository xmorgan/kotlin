/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.extensions

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder.buildValueParameter
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder.buildFunction
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder.buildBlockBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.*

private const val KOTLIN = "kotlin"
private const val AFU_PKG = "kotlinx/atomicfu"
private const val LOCKS = "locks"
private const val ATOMIC_CONSTRUCTOR = "atomic"
private const val ATOMICFU_VALUE_TYPE = """Atomic(Int|Long|Boolean|Ref)"""
private const val ATOMIC_ARRAY_TYPE = """Atomic(Int|Long|Boolean|)Array"""
private const val ATOMIC_ARRAY_FACTORY_FUNCTION = "atomicArrayOfNulls"
private const val ATOMICFU_RUNTIME_FUNCTION_PREDICATE = "atomicfu_"
private const val REENTRANT_LOCK_TYPE = "ReentrantLock"
private const val GETTER = "atomicfu\$getter"
private const val SETTER = "atomicfu\$setter"
private const val GET = "get"
private const val SET = "set"

private fun String.prettyStr() = replace('/', '.')

class AtomicFUTransformer(override val context: IrPluginContext) : IrElementTransformerVoid(), TransformerHelpers {

    private val irBuiltIns = context.irBuiltIns

    private val AFU_CLASSES: Map<String, IrType> = mapOf(
        "AtomicInt" to irBuiltIns.intType,
        "AtomicLong" to irBuiltIns.longType,
        "AtomicRef" to irBuiltIns.anyType,
        "AtomicBoolean" to irBuiltIns.booleanType
    )

    private val AFU_ARRAY_CLASSES: Map<String, String> = mapOf(
        "AtomicIntArray" to "IntArray",
        "AtomicLongArray" to "LongArray",
        "AtomicBooleanArray" to "BooleanArray",
        "AtomicArray" to "Array"
    )

    override fun visitFile(irFile: IrFile): IrFile {
        irFile.declarations.map { declaration ->
            declaration.transformAtomicInlineDeclaration()
        }
        return super.visitFile(irFile)
    }

    override fun visitClass(irClass: IrClass): IrStatement {
        irClass.declarations.map { declaration ->
            declaration.transformAtomicInlineDeclaration()
        }
        return super.visitClass(irClass)
    }

    override fun visitProperty(property: IrProperty): IrStatement {
        if (property.backingField != null) {
            val backingField = property.backingField!!
            if (backingField.initializer != null) {
                val initializer = backingField.initializer!!.expression.transformAtomicValueInitializer(backingField)
                property.backingField!!.initializer = IrExpressionBodyImpl(initializer)
            }
        }
        return super.visitProperty(property)
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        val parentFunction = (body as IrBodyBase<*>).container
        body.statements.forEachIndexed { i, stmt ->
            body.statements[i] = stmt.transformStatement(parentFunction)
        }
        return super.visitBlockBody(body)
    }

    private fun IrExpression.transformAtomicValueInitializer(parentFunction: IrDeclaration) =
        when {
            type.isAtomicValueType() -> getPureTypeValue().transformAtomicFunctionCall(parentFunction)
            type.isAtomicArrayType() -> buildPureTypeArrayConstructor()
            type.isReentrantLockType() -> buildConstNull()
            else -> this
        }

    private fun IrDeclaration.transformAtomicInlineDeclaration() {
        if (this is IrFunction &&
            isInline &&
            extensionReceiverParameter != null &&
            extensionReceiverParameter!!.type.isAtomicValueType()
        ) {
            val type = extensionReceiverParameter!!.type
            val valueType = type.atomicToValueType()
            val getterType = buildFunctionSimpleType(listOf(irBuiltIns.unitType, valueType))
            val setterType = buildFunctionSimpleType(listOf(valueType, irBuiltIns.unitType))
            val valueParametersCount = valueParameters.size
            val extendedValueParameters = valueParameters + listOf(
                buildValueParameter(Name.identifier(GETTER), valueParametersCount, getterType, IrDeclarationOrigin.DEFINED),
                buildValueParameter(Name.identifier(SETTER), valueParametersCount + 1, setterType, IrDeclarationOrigin.DEFINED)
            )
            extendedValueParameters.forEach { it.parent = this }
            this as IrSimpleFunction
            (descriptor as FunctionDescriptorImpl).initialize(
                null,
                descriptor.dispatchReceiverParameter,
                descriptor.typeParameters,
                extendedValueParameters.map {
                    it.descriptor as ValueParameterDescriptor
                },
                descriptor.returnType,
                descriptor.modality,
                descriptor.visibility
            )
            extensionReceiverParameter = null
            valueParameters = extendedValueParameters
        }
    }

    private fun IrExpression.getPureTypeValue(): IrExpression {
        require(this is IrCall && isAtomicFactoryFunction()) { "Illegal initializer for the atomic property $this" }
        return getValueArgument(0)!!
    }

    private fun IrExpression.buildPureTypeArrayConstructor() =
        when (this) {
            is IrConstructorCall -> {
                require(isAtomicArrayConstructor())
                val arrayConstructorSymbol = type.getArrayConstructorSymbol { it.owner.valueParameters.size == 1 }
                val size = getValueArgument(0)
                IrConstructorCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    irBuiltIns.unitType, arrayConstructorSymbol,
                    0, 0, 1
                ).apply {
                    putValueArgument(0, size)
                }
            }
            is IrCall -> {
                require(isAtomicArrayFactoryFunction()) { "Unsupported atomic array factory function $this" }
                val arrayFactorySymbol = referencePackageFunction("kotlin", "arrayOfNulls")
                val arrayElementType = getTypeArgument(0)!!
                val size = getValueArgument(0)
                buildCall(
                    target = arrayFactorySymbol,
                    type = type,
                    typeArguments = listOf(arrayElementType),
                    valueArguments = listOf(size)
                )
            }
            else -> error("Illegal type of atomic array initializer")
        }

    private fun IrCall.runtimeInlineAtomicFunctionCall(callReceiver: IrExpression, accessors: List<IrExpression>): IrCall {
        val valueArguments = getValueArguments()
        val functionName = getAtomicFunctionName()
        val receiverType = callReceiver.type.atomicToValueType()
        val runtimeFunction = getRuntimeFunctionSymbol(functionName, receiverType)
        return buildCall(
            target = runtimeFunction,
            type = type,
            origin = IrStatementOrigin.INVOKE,
            typeArguments = if (runtimeFunction.owner.typeParameters.size == 1) listOf(receiverType) else emptyList(),
            valueArguments = valueArguments + accessors
        )
    }

    private fun IrStatement.transformStatement(parentFunction: IrDeclaration) =
        when (this) {
            is IrExpression -> transformAtomicFunctionCall(parentFunction)
            is IrVariable -> {
                apply { initializer = initializer?.transformAtomicFunctionCall(parentFunction) }
            }
            else -> this
        }

    private fun IrExpression.transformAtomicFunctionCall(parentFunction: IrDeclaration): IrExpression {
        // erase unchecked cast to the Atomic* type
        if (this is IrTypeOperatorCall && operator == IrTypeOperator.CAST && typeOperand.isAtomicValueType()) {
            return argument
        }
        if (isAtomicValueInitializerCall()) {
            return transformAtomicValueInitializer(parentFunction)
        }
        when (this) {
            is IrTypeOperatorCall -> {
                return apply { argument = argument.transformAtomicFunctionCall(parentFunction) }
            }
            is IrReturn -> {
                return apply { value = value.transformAtomicFunctionCall(parentFunction) }
            }
            is IrSetVariable -> {
                return apply { value = value.transformAtomicFunctionCall(parentFunction) }
            }
            is IrSetField -> {
                return apply { value = value.transformAtomicFunctionCall(parentFunction) }
            }
            is IrIfThenElseImpl -> {
                return apply {
                    branches.forEachIndexed { i, branch ->
                        branches[i] = branch.apply {
                            condition = condition.transformAtomicFunctionCall(parentFunction)
                            result = result.transformAtomicFunctionCall(parentFunction)
                        }
                    }
                }
            }
            is IrTry -> {
                return apply {
                    tryResult = tryResult.transformAtomicFunctionCall(parentFunction)
                    catches.forEach {
                        it.result = it.result.transformAtomicFunctionCall(parentFunction)
                    }
                    finallyExpression = finallyExpression?.transformAtomicFunctionCall(parentFunction)
                }
            }
            is IrBlock -> {
                return apply {
                    statements.forEachIndexed { i, stmt ->
                        statements[i] = stmt.transformStatement(parentFunction)
                    }
                }
            }
            is IrCall -> {
                if (dispatchReceiver != null) {
                    dispatchReceiver = dispatchReceiver!!.transformAtomicFunctionCall(parentFunction)
                }
                getValueArguments().forEachIndexed { i, arg ->
                    putValueArgument(i, arg?.transformAtomicFunctionCall(parentFunction) as IrExpression)
                }
                val isInline = symbol.owner.isInline
                val callReceiver = extensionReceiver ?: dispatchReceiver ?: return this
                if (symbol.isKotlinxAtomicfuPackage() && callReceiver.type.isAtomicValueType()) {
                    // 1. transform function call on the atomic class field
                    if (callReceiver is IrCall) {
                        val accessors = callReceiver.getPropertyAccessors(parentFunction)
                        return runtimeInlineAtomicFunctionCall(callReceiver, accessors)
                    }
                    // 2. transform function call on the atomic extension receiver
                    if (callReceiver is IrGetValue) {
                        val containingDeclaration = callReceiver.symbol.owner.parent as IrFunction
                        val accessorParameters = containingDeclaration.valueParameters.takeLast(2).map { it.capture() }
                        return runtimeInlineAtomicFunctionCall(callReceiver, accessorParameters)
                    }
                }
                // 3. transform inline Atomic* extension function call
                if (isInline && callReceiver is IrCall && callReceiver.type.isAtomicValueType()) {
                    val accessors = callReceiver.getPropertyAccessors(parentFunction)
                    val dispatch = dispatchReceiver
                    val args = getValueArguments()
                    return buildCall(
                        target = symbol,
                        type = type,
                        origin = IrStatementOrigin.INVOKE,
                        valueArguments = args + accessors
                    ).apply {
                        dispatchReceiver = dispatch
                    }
                }
            }
        }
        return this
    }

    private fun IrExpression.isAtomicValueInitializerCall() =
        (this is IrCall && (this.isAtomicFactoryFunction() || this.isAtomicArrayFactoryFunction())) ||
                (this is IrConstructorCall && this.isAtomicArrayConstructor()) ||
                type.isReentrantLockType()

    private fun IrCall.isArrayElementGetter() =
        dispatchReceiver != null &&
                dispatchReceiver!!.type.isAtomicArrayType() &&
                symbol.owner.name.asString() == GET

    private fun IrCall.buildGetterLambda(parentFunction: IrDeclaration): IrExpression {
        val isArrayElement = isArrayElementGetter()
        val getterCall = if (isArrayElement) dispatchReceiver as IrCall else this
        val valueType = type.atomicToValueType()
        val getField = buildGetField(getterCall.getBackingField(), getterCall.dispatchReceiver)
        val getterType = buildFunctionSimpleType(listOf(context.irBuiltIns.unitType, valueType))
        val getterBody = if (isArrayElement) {
            val getSymbol = referenceFunction(getterCall.type.referenceClass(), GET)
            val elementIndex = getValueArgument(0)!!.deepCopyWithVariables()
            buildCall(
                target = getSymbol,
                type = valueType,
                origin = IrStatementOrigin.LAMBDA,
                valueArguments = listOf(elementIndex)
            ).apply {
                dispatchReceiver = getField.deepCopyWithVariables()
            }
        } else {
            getField.deepCopyWithVariables()
        }
        return buildAccessorLambda(
            name = getterName(getterCall.symbol.owner.name.getFieldName()!!),
            type = getterType,
            returnType = valueType,
            valueParameters = emptyList(),
            body = getterBody,
            parentFunction = parentFunction
        )
    }

    private fun IrCall.buildSetterLambda(parentFunction: IrDeclaration): IrExpression {
        val isArrayElement = isArrayElementGetter()
        val getterCall = if (isArrayElement) dispatchReceiver as IrCall else this
        val valueType = type.atomicToValueType()
        val valueParameter = buildValueParameter(index = 0, type = valueType)
        val setterType = buildFunctionSimpleType(listOf(valueType, context.irBuiltIns.unitType))
        val setterBody = if (isArrayElement) {
            val setSymbol = referenceFunction(getterCall.type.referenceClass(), SET)
            val elementIndex = getValueArgument(0)!!.deepCopyWithVariables()
            buildCall(
                target = setSymbol,
                type = context.irBuiltIns.unitType,
                origin = IrStatementOrigin.LAMBDA,
                valueArguments = listOf(elementIndex, valueParameter.capture())
            ).apply {
                dispatchReceiver = getterCall
            }
        } else {
            buildSetField(getterCall.getBackingField(), getterCall.dispatchReceiver, valueParameter.capture())
        }
        return buildAccessorLambda(
            name = setterName(getterCall.symbol.owner.name.getFieldName()!!),
            type = setterType,
            returnType = context.irBuiltIns.unitType,
            valueParameters = listOf(valueParameter),
            body = setterBody,
            parentFunction = parentFunction
        )
    }

    private fun IrCall.getBackingField(): IrField {
        val correspondingPropertySymbol = (symbol.owner as IrFunctionImpl).correspondingPropertySymbol!!
        return correspondingPropertySymbol.owner.backingField!!
    }

    private fun buildAccessorLambda(
        name: String,
        type: IrType,
        returnType: IrType,
        valueParameters: List<IrValueParameter>,
        body: IrExpression,
        parentFunction: IrDeclaration
    ): IrFunctionExpression {
        val accessorFunction = buildFunction(
            name = name,
            returnType = returnType,
            parent = parentFunction as IrDeclarationParent,
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
            isInline = true
        ).apply {
            valueParameters.forEach { it.parent = this }
            this.valueParameters = valueParameters
            this.body = buildBlockBody(listOf(body))
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        }
        return IrFunctionExpressionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type,
            accessorFunction,
            IrStatementOrigin.LAMBDA
        )
    }

    private fun buildSetField(backingField: IrField, ownerClass: IrExpression?, value: IrGetValue): IrSetField {
        val receiver = if (ownerClass is IrTypeOperatorCall) ownerClass.argument as IrGetValue else ownerClass
        val fieldSymbol = backingField.symbol
        return buildSetField(
            symbol = fieldSymbol,
            receiver = receiver,
            value = value
        )
    }

    private fun buildGetField(backingField: IrField, ownerClass: IrExpression?): IrGetField {
        val receiver = if (ownerClass is IrTypeOperatorCall) ownerClass.argument as IrGetValue else ownerClass
        return buildGetField(backingField.symbol, receiver)
    }

    private fun IrCall.getPropertyAccessors(parentFunction: IrDeclaration): List<IrExpression> =
        listOf(buildGetterLambda(parentFunction), buildSetterLambda(parentFunction))

    private fun getRuntimeFunctionSymbol(name: String, type: IrType): IrSimpleFunctionSymbol {
        val functionName = when (name) {
            "value.<get-value>" -> "getValue"
            "value.<set-value>" -> "setValue"
            else -> name
        }
        return referencePackageFunction("kotlin.js", "$ATOMICFU_RUNTIME_FUNCTION_PREDICATE$functionName") {
            val typeArg = (it.owner.getGetterParameter().type as IrSimpleType).arguments.first()
            !(typeArg as IrType).isPrimitiveType() || typeArg == type
        }
    }

    private fun IrFunction.getGetterParameter() = valueParameters[valueParameters.lastIndex - 1]

    private fun IrCall.isAtomicFactoryFunction(): Boolean {
        val name = symbol.owner.name
        return !name.isSpecial && name.identifier == ATOMIC_CONSTRUCTOR
    }

    private fun IrCall.isAtomicArrayFactoryFunction(): Boolean {
        val name = symbol.owner.name
        return !name.isSpecial && name.identifier == ATOMIC_ARRAY_FACTORY_FUNCTION
    }

    private fun IrConstructorCall.isAtomicArrayConstructor(): Boolean {
        val name = (type as IrSimpleType).classifier.descriptor.name
        return !name.isSpecial && name.identifier.matches(Regex(ATOMIC_ARRAY_TYPE))
    }

    private fun IrSymbol.isKotlinxAtomicfuPackage() =
        this is IrPublicSymbolBase<*> && signature.packageFqName().asString() == AFU_PKG.prettyStr()

    private fun IrType.isAtomicValueType() = belongsTo(ATOMICFU_VALUE_TYPE)
    private fun IrType.isAtomicArrayType() = belongsTo(ATOMIC_ARRAY_TYPE)
    private fun IrType.isReentrantLockType() = belongsTo("$AFU_PKG/$LOCKS", REENTRANT_LOCK_TYPE)

    private fun IrType.belongsTo(typeName: String) = belongsTo(AFU_PKG, typeName)

    private fun IrType.belongsTo(packageName: String, typeName: String): Boolean {
        if (this !is IrSimpleType || classifier !is IrClassPublicSymbolImpl) return false
        val signature = classifier.signature as IdSignature.PublicSignature
        val pckg = signature.packageFqName().asString()
        val type = signature.declarationFqn.asString()
        return pckg == packageName.prettyStr() && type.matches(typeName.toRegex())
    }

    private fun IrCall.getAtomicFunctionName(): String {
        val signature = symbol.signature
        val classFqn = if (signature is IdSignature.AccessorSignature) {
            signature.accessorSignature.declarationFqn
        } else (signature as IdSignature.PublicSignature).declarationFqn
        val pattern = "$ATOMICFU_VALUE_TYPE\\.(.*)".toRegex()
        val declarationName = classFqn.asString()
        return pattern.findAll(declarationName).firstOrNull()?.let { it.groupValues[2] } ?: declarationName
    }

    private fun IrType.atomicToValueType(): IrType {
        require(isAtomicValueType())
        val classId = ((this as IrSimpleType).classifier.signature as IdSignature.PublicSignature).declarationFqn.asString()
        return AFU_CLASSES[classId]!!
    }

    private fun buildConstNull() = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.anyType)

    private fun IrType.getArrayConstructorSymbol(predicate: (IrConstructorSymbol) -> Boolean = { true }): IrConstructorSymbol {
        val afuClassId = ((this as IrSimpleType).classifier.signature as IdSignature.PublicSignature).declarationFqn.asString()
        val classId = FqName("$KOTLIN.${AFU_ARRAY_CLASSES[afuClassId]!!}")
        return context.referenceConstructors(classId).single(predicate)
    }

    private fun IrType.referenceClass(): IrClassSymbol {
        val afuClassId = ((this as IrSimpleType).classifier.signature as IdSignature.PublicSignature).declarationFqn.asString()
        val classId = FqName("$KOTLIN.${AFU_ARRAY_CLASSES[afuClassId]!!}")
        return context.referenceClass(classId)!!
    }

    private fun referencePackageFunction(
        packageName: String,
        name: String,
        predicate: (IrFunctionSymbol) -> Boolean = { true }
    ) = context.referenceFunctions(FqName("$packageName.$name")).single(predicate)

    private fun referenceFunction(classSymbol: IrClassSymbol, functionName: String): IrFunctionSymbol {
        val functionId = FqName("$KOTLIN.${classSymbol.owner.name}.$functionName")
        return context.referenceFunctions(functionId).single()
    }

    companion object {
        fun transform(irFile: IrFile, context: IrPluginContext) =
            irFile.transform(AtomicFUTransformer(context), null)
    }
}
