package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaClassResolver
import org.jetbrains.jet.utils.emptyOrSingletonList
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaTypeParameterResolver

class LazyJavaClassDescriptor(
        private val storageManager: StorageManager,
        private val typeResolver: LazyJavaTypeResolver,
        containingDeclaration: DeclarationDescriptor,
        fqName: FqName,
        private val javaClass: JavaClass
) : ClassDescriptorBase(containingDeclaration, fqName.shortName()), LazyJavaDescriptor {

    private val _kind = JavaClassResolver.determineClassKind(javaClass)
    private val _modality = JavaClassResolver.determineClassModality(javaClass)
    private val _visibility = javaClass.getVisibility()
    private val _isInner = JavaClassResolver.isInnerClass(javaClass)

    override fun getKind() = _kind
    override fun getModality() = _modality
    override fun getVisibility() = _visibility
    override fun isInner() = _isInner

    private val _typeConstructor = storageManager.createLazyValue { LazyJavaClassTypeConstructor() }
    override fun getTypeConstructor() = _typeConstructor()

    private val _scopeForMemberLookup = storageManager.createLazyValue {
        // TODO
        throw UnsupportedOperationException()
    }

    override fun getScopeForMemberLookup() = _scopeForMemberLookup()

    private val _thisAsReceiverParameter = storageManager.createLazyValue { DescriptorFactory.createLazyReceiverParameterDescriptor(this) }
    override fun getThisAsReceiverParameter() = _thisAsReceiverParameter()

    // TODO
    override fun getUnsubstitutedInnerClassesScope(): JetScope = JetScope.EMPTY

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    // TODO
    override fun getConstructors() = emptyOrSingletonList(getUnsubstitutedPrimaryConstructor())

    // TODO
    override fun getClassObjectType(): JetType? = null

    // TODO
    override fun getClassObjectDescriptor(): ClassDescriptor? = null

    // TODO
    override fun getAnnotations(): List<AnnotationDescriptor> = Collections.emptyList()

    private inner class LazyJavaClassTypeConstructor : TypeConstructor {

        //private val parameters = storageManager.createLazyValue {
        //    LazyJavaTypeParameterResolver(storageManager, this@LazyJavaClassDescriptor, predicate, )
        //}

        override fun getParameters(): List<TypeParameterDescriptor> {
            // TODO
            throw UnsupportedOperationException()
        }

        override fun getSupertypes(): Collection<JetType> {
            // TODO
            throw UnsupportedOperationException()
        }

        override fun getAnnotations() = Collections.emptyList<AnnotationDescriptor>()

        override fun isFinal() = !getModality().isOverridable()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

    }
}