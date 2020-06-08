/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.asJava.propertyNameByAccessor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.type.MapPsiToAsmDesc
import javax.swing.Icon

inline fun <T, reified R> Array<out T>.map2Array(transform: (T) -> R): Array<R> =
    Array(this.size) { i -> transform(this[i]) }

class KtLightElementImpl<T : PsiMember>(private val origin: LightMemberOriginForCompiledElement<T>, parent: PsiElement) :
    KtLightElementBase(parent) {
    override val kotlinOrigin: KtElement?
        get() = origin.originalElement

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        this == another || origin.isEquivalentTo(another)
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
open class PsiFieldWrapper(private val fldDelegate: PsiField, private val parent: PsiElement) :
    PsiField by fldDelegate {

//    private val _modifierList: PsiModifierList? by lazyPub {
//        fldDelegate.modifierList?.let { PsiModifierListWrapper(it, this) }
//    }
//
//    override fun getModifierList(): PsiModifierList? = _modifierList

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        this == another || (another is PsiFieldWrapper && fldDelegate.isEquivalentTo(another.fldDelegate))


    override fun getParent(): PsiElement = parent
    override fun copy(): PsiElement = this
    override fun toString(): String = fldDelegate.toString()
    override fun hashCode(): Int = fldDelegate.hashCode()
    override fun equals(other: Any?): Boolean =
        this === other || other is PsiFieldWrapper && other.fldDelegate == fldDelegate
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
open class PsiClassWrapper(private val clsDelegate: PsiClass, private val parent: PsiElement) :
    PsiClass by clsDelegate {

//    private val _modifierList: PsiModifierList? by lazyPub {
//        clsDelegate.modifierList?.let { PsiModifierListWrapper(it, this) }
//    }
//
//    override fun getModifierList(): PsiModifierList? = _modifierList

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        this == another || (another is PsiClassWrapper && clsDelegate.isEquivalentTo(another.clsDelegate))

    override fun getParent(): PsiElement = parent
    override fun copy(): PsiElement = this
    override fun toString(): String = clsDelegate.toString()
    override fun hashCode(): Int = clsDelegate.hashCode()
    override fun equals(other: Any?): Boolean =
        this === other || other is PsiClassWrapper && other.clsDelegate == clsDelegate

}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
open class PsiMethodWrapper(private val funDelegate: PsiMethod, private val parent: PsiElement) :
    PsiMethod by funDelegate, PsiAnnotationMethod {

//    private val _parameterList: PsiParameterList by lazyPub {
//        PsiParameterListWrapper(funDelegate.parameterList, this)
//    }
//
//    override fun getParameterList(): PsiParameterList = _parameterList

//    private val _modifierList: PsiModifierList by lazyPub {
//        PsiModifierListWrapper(funDelegate.modifierList, this)
//    }

//    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getParent(): PsiElement = parent
    override fun copy(): PsiElement = this
    override fun toString(): String = funDelegate.toString()

    override fun getDefaultValue(): PsiAnnotationMemberValue? = (funDelegate as? PsiAnnotationMethod)?.defaultValue

    override fun hashCode(): Int = funDelegate.hashCode()
    override fun equals(other: Any?): Boolean =
        this === other || other is PsiMethodWrapper && other.funDelegate == funDelegate
}

//@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
//class PsiParameterWrapper(private val delegate: PsiParameter, private val parent: PsiParameterList) :
//    PsiParameter by delegate {
//
//    private val _modifierList: PsiModifierList? by lazyPub {
//        delegate.modifierList?.let { PsiModifierListWrapper(it, this) }
//    }
//
//    override fun getModifierList(): PsiModifierList? = _modifierList
//
//    override fun getParent(): PsiElement = parent
//    override fun copy(): PsiElement = this
//    override fun toString(): String = delegate.toString()
//    override fun hashCode(): Int = delegate.hashCode()
//    override fun equals(other: Any?): Boolean =
//        this === other || other is PsiParameterWrapper && other.delegate == delegate
//}
//
//@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
//class PsiParameterListWrapper(private val delegate: PsiParameterList, private val parent: PsiParameterListOwner) :
//    PsiParameterList by delegate {
//
////    private val _parameters: Array<PsiParameter> by lazyPub {
////        delegate.parameters.map2Array {
////            PsiParameterWrapper(it, this)
////        }
////    }
//
//    override fun getParameters(): Array<PsiParameter> = _parameters
//
//    override fun getParent(): PsiElement = parent
//    override fun copy(): PsiElement = this
//    override fun toString(): String = delegate.toString()
//    override fun hashCode(): Int = delegate.hashCode()
//    override fun equals(other: Any?): Boolean =
//        this === other || other is PsiParameterListWrapper && other.delegate == delegate
//}

//@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
//class PsiModifierListWrapper(private val delegate: PsiModifierList, private val parent: PsiModifierListOwner) :
//    PsiModifierList by delegate {
//
//    override fun getParent(): PsiElement = parent
//    override fun copy(): PsiElement = this
//    override fun toString(): String = delegate.toString()
//    override fun hashCode(): Int = delegate.hashCode()
//    override fun equals(other: Any?): Boolean =
//        this === other || other is PsiModifierListWrapper && other.delegate == delegate
//}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class PsiFieldLightElementWrapper(
    private val fldDelegate: PsiField,
    private val parent: PsiElement,
    val lightElementDelegate: KtLightElementImpl<PsiField>,
) :
    PsiFieldWrapper(fldDelegate, parent),
    NavigatablePsiElement by lightElementDelegate,
    Iconable by lightElementDelegate,
    Cloneable {

    override fun getName(): String = super.getName()
    override fun getIcon(flags: Int): Icon? = lightElementDelegate.getIcon(flags)
    override fun getNavigationElement() = lightElementDelegate.kotlinOrigin?.navigationElement ?: parent
    override fun hashCode(): Int = super.hashCode()
    override fun equals(other: Any?): Boolean =
        super.equals(other) &&
                other is PsiFieldLightElementWrapper &&
                lightElementDelegate.kotlinOrigin == other.lightElementDelegate.kotlinOrigin

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) ||
                lightElementDelegate.isEquivalentTo(another) ||
                (another is KtLightMember<*> && lightElementDelegate.isEquivalentTo(another.lightMemberOrigin?.originalElement))
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class PsiMethodLightElementWrapper(
    funDelegate: PsiMethod,
    private val parent: PsiElement,
    val lightElementDelegate: KtLightElementImpl<PsiMethod>,
) :
    PsiMethodWrapper(funDelegate, parent),
    NavigatablePsiElement by lightElementDelegate,
    Iconable by lightElementDelegate,
    Cloneable {

    override fun getName(): String = super.getName()
    override fun getIcon(flags: Int): Icon? = lightElementDelegate.getIcon(flags)
    override fun getNavigationElement() = lightElementDelegate.kotlinOrigin?.navigationElement ?: parent
    override fun hashCode(): Int = super.hashCode()
    override fun equals(other: Any?): Boolean =
        super.equals(other) &&
                other is PsiMethodLightElementWrapper &&
                lightElementDelegate.kotlinOrigin == other.lightElementDelegate.kotlinOrigin

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) ||
                lightElementDelegate.isEquivalentTo(another) ||
                (another is KtLightMember<*> && lightElementDelegate.isEquivalentTo(another.lightMemberOrigin?.originalElement))
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class FUN(
    private val funDelegate: PsiMethod,
    private val funParent: KtLightClass,
    file: KtClsFile,
    override val kotlinOrigin: KtNamedFunction?
) : PsiMethodWrapper(funDelegate, funParent), KtLightMethod, KtLightMember<PsiMethod>,
    ElementSupportTrait<KtNamedFunction, KtLightClass> by ElementSupportTrait.Instance(kotlinOrigin, funParent) {

    //CP
    override val isMangled: Boolean
        get() {
            val demangledName = KotlinTypeMapper.InternalNameMapper.demangleInternalName(name) ?: return false
            val originalName = propertyNameByAccessor(demangledName, this) ?: demangledName
            return originalName == kotlinOrigin?.name
        }

    override fun getPresentation(): ItemPresentation? =
        (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }


    override fun equals(other: Any?): Boolean = other is FUN && kotlinOrigin == other.kotlinOrigin
    override fun hashCode(): Int = funDelegate.hashCode()
    override fun copy(): PsiElement = this
    override fun toString(): String = "${this.javaClass.simpleName} of $parent"

    override val clsDelegate: PsiMethod = funDelegate
    override val lightMemberOrigin: LightMemberOrigin? = LightMemberOriginForCompiledMethod(funDelegate, file)

    override fun getContainingClass(): KtLightClass = funParent
}


@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class FLD(
    private val fldDelegate: PsiField,
    private val fldParent: KtLightClass,
    file: KtClsFile,
    override val kotlinOrigin: KtDeclaration?
) : PsiFieldWrapper(fldDelegate, fldParent), KtLightField, KtLightMember<PsiField>,
    ElementSupportTrait<KtDeclaration, KtLightClass> by ElementSupportTrait.Instance(kotlinOrigin, fldParent) {

    override fun getPresentation(): ItemPresentation? =
        (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }

    override fun equals(other: Any?): Boolean = other is FUN && kotlinOrigin == other.kotlinOrigin
    override fun hashCode(): Int = fldDelegate.hashCode()
    override fun copy(): PsiElement = this
    override fun toString(): String = "${this.javaClass.simpleName} of $parent"

    override val clsDelegate: PsiField = fldDelegate
    override val lightMemberOrigin: LightMemberOrigin? = LightMemberOriginForCompiledField(fldDelegate, file)

    override fun computeConstantValue(p0: MutableSet<PsiVariable>?): Any? = fldDelegate.computeConstantValue()

    override fun getContainingClass(): KtLightClass = fldParent
}



interface ElementSupportTrait<O : KtDeclaration?, P : PsiElement>: Cloneable{

    class Instance<O1 : KtDeclaration, P1 : PsiElement>(
        override val kotlinOrigin: O1?,
        override val parentElement: P1
    ) : ElementSupportTrait<O1, P1> {
        override fun isEquivalentTo(another: PsiElement?): Boolean {
            return this == another ||
                    kotlinOrigin?.let { another is ElementSupportTrait<*, *> && it.isEquivalentTo(another.kotlinOrigin) } ?: false
        }

        override fun clone(): Any = this
        override fun getParent(): PsiElement = parentElement
        override fun getText(): String = kotlinOrigin?.text ?: ""
        override fun getTextRange(): TextRange = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE
        override fun getTextOffset() = kotlinOrigin?.textOffset ?: 0
        override fun getStartOffsetInParent() = kotlinOrigin?.startOffsetInParent ?: 0
        override fun isWritable(): Boolean = false
        override fun getUseScope() = kotlinOrigin?.useScope ?: parentElement.useScope
        override fun getContainingFile(): PsiFile = parentElement.containingFile
        override fun isValid(): Boolean = parentElement.isValid && (kotlinOrigin?.isValid != false)

        override fun findElementAt(offset: Int): PsiElement? = kotlinOrigin?.findElementAt(offset)
        override fun getNavigationElement(): PsiElement = kotlinOrigin?.navigationElement ?: parentElement
    }

    val kotlinOrigin: O?
    val parentElement: P

    fun isEquivalentTo(another: PsiElement?): Boolean
    fun getParent(): PsiElement
    fun getText(): String
    fun getTextRange(): TextRange
    fun getTextOffset(): Int
    fun getStartOffsetInParent(): Int
    fun isWritable(): Boolean
    fun getUseScope(): SearchScope
    fun getContainingFile(): PsiFile
    fun isValid(): Boolean
    fun findElementAt(offset: Int): PsiElement?
    fun getNavigationElement(): PsiElement
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class KtLightClassForDecompiledDeclaration(
    override val clsDelegate: PsiClass,
    private val clsParent: PsiElement,
    private val file: KtClsFile,
    kotlinOrigin: KtClassOrObject?
) : KtLightClassBase(clsDelegate.manager), Cloneable,
    ElementSupportTrait<KtClassOrObject, PsiElement> by ElementSupportTrait.Instance(kotlinOrigin, clsParent) {

    constructor(
        clsDelegate: PsiClass,
        kotlinOrigin: KtClassOrObject?,
        file: KtClsFile
    ) : this(
        clsDelegate = clsDelegate,
        clsParent = file,
        file = file,
        kotlinOrigin = kotlinOrigin
    )

    val fqName = kotlinOrigin?.fqName ?: FqName(clsDelegate.qualifiedName.orEmpty())

    private fun findMethodDeclaration(psiMethod: PsiMethod): KtNamedFunction? {
        val desc = MapPsiToAsmDesc.methodDesc(psiMethod)
        val name = if (psiMethod.isConstructor) "<init>" else psiMethod.name
        val signature = MemberSignature.fromMethodNameAndDesc(name, desc)
        return findDeclarationInCompiledFile(file, psiMethod, signature) as? KtNamedFunction
    }

    private fun findFieldDeclaration(psiField: PsiField): KtDeclaration? {
        val desc = MapPsiToAsmDesc.typeDesc(psiField.type)
        val signature = MemberSignature.fromFieldNameAndDesc(psiField.name, desc)
        return findDeclarationInCompiledFile(file, psiField, signature)
    }

    private val _methods: List<PsiMethod> by lazyPub {
        clsDelegate.methods.map { psiMethod ->
            FUN(
                funDelegate = psiMethod,
                funParent = this,
                file = file,
                kotlinOrigin = findMethodDeclaration(psiMethod)
            )
        }
    }

    private val _fields: List<PsiField> by lazyPub {
        clsDelegate.fields.map { psiField ->
            FLD(
                fldDelegate = psiField,
                fldParent = this,
                file = file,
                kotlinOrigin = findFieldDeclaration(psiField)
            )
        }
    }

    override fun getOwnFields(): List<PsiField> = _fields
    override fun getOwnMethods(): List<PsiMethod> = _methods
    override fun getOwnInnerClasses(): List<PsiClass> = _innerClasses

    private val _innerClasses: List<PsiClass> by lazyPub {
        clsDelegate.innerClasses.map { psiClass ->
            val innerDeclaration = kotlinOrigin
                ?.declarations
                ?.filterIsInstance<KtClassOrObject>()
                ?.firstOrNull { it.name == clsDelegate.name }

            KtLightClassForDecompiledDeclaration(
                clsDelegate = psiClass,
                clsParent = this,
                file = file,
                kotlinOrigin = innerDeclaration
            )
        }
    }

    override fun equals(other: Any?): Boolean = other is KtLightClassForDecompiledDeclaration && kotlinOrigin == other.kotlinOrigin
    override fun hashCode(): Int = clsDelegate.hashCode()
    override fun copy(): PsiElement = this
    override fun toString(): String = "${this.javaClass.simpleName} of $parent"

    override fun getPresentation(): ItemPresentation? =
        (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }

    override val originKind: LightClassOriginKind = LightClassOriginKind.BINARY
}






    class KtLightClassForDecompiledDeclaration1(
        override val clsDelegate: ClsClassImpl,
        override val kotlinOrigin: KtClassOrObject?,
        private val file: KtClsFile,
    ) : KtLightClassBase(clsDelegate.manager) {
        val fqName = kotlinOrigin?.fqName ?: FqName(clsDelegate.qualifiedName.orEmpty())

        override fun copy() = this

        override fun getOwnInnerClasses(): List<PsiClass> {
            val nestedClasses = kotlinOrigin?.declarations?.filterIsInstance<KtClassOrObject>() ?: emptyList()
            return clsDelegate.ownInnerClasses.map { innerClsClass ->
                KtLightClassForDecompiledDeclaration(
                    innerClsClass as ClsClassImpl,
                    nestedClasses.firstOrNull { innerClsClass.name == it.name }, file,
                )
            }
        }

        override fun getOwnFields(): List<PsiField> {
            return clsDelegate.ownFields.map { KtLightFieldImpl.create(LightMemberOriginForCompiledField(it, file), it, this) }
        }

        override fun getOwnMethods(): List<PsiMethod> {
            return clsDelegate.ownMethods.map { KtLightMethodImpl.create(it, LightMemberOriginForCompiledMethod(it, file), this) }
        }

        override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: file

        override fun getParent() = clsDelegate.parent

        override fun equals(other: Any?): Boolean =
            other is KtLightClassForDecompiledDeclaration &&
                    fqName == other.fqName

        override fun hashCode(): Int =
            fqName.hashCode()

        override val originKind: LightClassOriginKind
            get() = LightClassOriginKind.BINARY
    }

