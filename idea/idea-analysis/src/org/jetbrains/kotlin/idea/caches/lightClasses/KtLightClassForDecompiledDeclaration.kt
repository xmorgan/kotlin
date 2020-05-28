/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses

import com.intellij.openapi.util.Iconable
import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.util.PsiClassUtil
import com.intellij.util.containers.map2Array
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import javax.swing.Icon

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
    PsiMethod by funDelegate {

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
open class PsiClassLightElementWrapper(
    private val clsDelegate: PsiClass,
    private val parent: PsiElement,
    val lightElementDelegate: KtLightElementImpl<PsiClass>,
    protected val file: KtClsFile,
) :
    PsiClassWrapper(clsDelegate, parent),
    NavigatablePsiElement by lightElementDelegate,
    Iconable by lightElementDelegate,
    Cloneable {

    override fun getName(): String? = super.getName()
    override fun getIcon(flags: Int): Icon? = lightElementDelegate.getIcon(flags)
    override fun getNavigationElement() = lightElementDelegate.kotlinOrigin?.navigationElement ?: parent
    override fun hashCode(): Int = super.hashCode()

    override fun equals(other: Any?): Boolean =
        other is PsiClassLightElementWrapper &&
                super.equals(other) &&
                lightElementDelegate.kotlinOrigin == other.lightElementDelegate.kotlinOrigin

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return this == another ||
                lightElementDelegate.isEquivalentTo(another) ||
                (another is KtLightMember<*> && lightElementDelegate.isEquivalentTo(another.lightMemberOrigin?.originalElement))
    }

    private val _methods: Array<PsiMethod> by lazyPub {
        clsDelegate.methods.map2Array {
            val elementImpl = KtLightElementImpl(LightMemberOriginForCompiledMethod(it, file), this)
            PsiMethodLightElementWrapper(it, this, elementImpl)
        }
    }
    private val _fields: Array<PsiField> by lazyPub {
        clsDelegate.fields.map2Array {
            val elementImpl = KtLightElementImpl(LightMemberOriginForCompiledField(it, file), this)
            PsiFieldLightElementWrapper(it, this, elementImpl)
        }
    }
    private val _constructors: Array<PsiMethod> by lazyPub {
        PsiImplUtil.getConstructors(this)
    }

    override fun getConstructors(): Array<PsiMethod> = _constructors

    protected open fun createClassWrapper(
        clsDelegate: PsiClass,
        kotlinOrigin: KtClassOrObject?,
    ): PsiClassLightElementWrapper {

        val elementDelegate = KtLightElementImpl(
            origin = LightMemberOriginForCompiledClass(kotlinOrigin, clsDelegate),
            parent = this
        )

        return PsiClassLightElementWrapper(
            clsDelegate = clsDelegate,
            parent = this,
            lightElementDelegate = elementDelegate,
            file = file
        )
    }

    private val _innerClasses: Array<PsiClass> by lazyPub {
        clsDelegate.innerClasses.map2Array { psiClass ->
            val innerDeclaration = (lightElementDelegate.kotlinOrigin as? KtClassOrObject)
                ?.declarations
                ?.filterIsInstance<KtClassOrObject>()
                ?.firstOrNull { it.name == clsDelegate.name }

            createClassWrapper(psiClass, innerDeclaration)
        }
    }

    override fun getInnerClasses(): Array<PsiClass> = _innerClasses
    override fun getMethods(): Array<PsiMethod> = _methods
    override fun getFields(): Array<PsiField> = _fields
}

class KtLightClassForDecompiledDeclaration(
    override val clsDelegate: PsiClass,
    override val kotlinOrigin: KtClassOrObject?,
    file: KtClsFile,
) : PsiClassLightElementWrapper(
    clsDelegate = clsDelegate,
    parent = file,
    lightElementDelegate = KtLightElementImpl(
        origin = LightMemberOriginForCompiledClass(element = kotlinOrigin, psiClass = clsDelegate),
        parent = file,
    ),
    file = file,
), KtLightClass {
    val fqName = kotlinOrigin?.fqName ?: FqName(clsDelegate.qualifiedName.orEmpty())

    override val originKind: LightClassOriginKind = LightClassOriginKind.BINARY

    override fun createClassWrapper(clsDelegate: PsiClass, kotlinOrigin: KtClassOrObject?): PsiClassLightElementWrapper =
        KtLightClassForDecompiledDeclaration(clsDelegate, kotlinOrigin, file)
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

