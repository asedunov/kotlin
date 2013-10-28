package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.structure.*
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationArgumentResolver
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver.*
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.types.TypeProjection
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyType
import org.jetbrains.jet.utils.valuesToMap
import org.jetbrains.jet.utils.keysToMap
import org.jetbrains.jet.utils.keysToMapExceptNulls
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap

private object DEPRECATED_IN_JAVA : JavaLiteralAnnotationArgument {
    override fun getName(): Name? = null
    override fun getValue(): Any? = "Deprecated in Java"
}

class LazyJavaAnnotationDescriptor(
        private val c: LazyJavaResolverContextWithTypes,
        val javaAnnotation : JavaAnnotation
) : AnnotationDescriptor {

    private val _fqName = c.storageManager.createLazyValue { javaAnnotation.getFqName() }
    private val _type = c.storageManager.createLazyValue {() : JetType ->
        val fqName = _fqName()
        if (fqName == null) return@createLazyValue ErrorUtils.createErrorType("No fqName: $javaAnnotation")
        val annotationClass = JavaToKotlinClassMap.getInstance().mapKotlinClass(fqName, TypeUsage.MEMBER_SIGNATURE_INVARIANT)
                                ?: c.javaClassResolver.resolveClassByFqName(fqName)
        annotationClass?.getDefaultType() ?: ErrorUtils.createErrorType(fqName.asString())
    }

    override fun getType(): JetType = _type()

    private val _nameToArgument = c.storageManager.createLazyValue {
        var arguments: Collection<JavaAnnotationArgument> = javaAnnotation.getArguments()
        if (arguments.isEmpty() && _fqName() == JavaToKotlinClassMap.JAVA_LANG_DEPRECATED) {
            arguments = listOf(DEPRECATED_IN_JAVA)
        }
        arguments.valuesToMap { a -> a.getName() }
    }

    private val _valueArguments = c.storageManager.createMemoizedFunctionWithNullableValues<ValueParameterDescriptor, CompileTimeConstant<out Any?>> {
        valueParameter ->
        val nameToArg = _nameToArgument()

        var javaAnnotationArgument = nameToArg[valueParameter.getName()]
        if (javaAnnotationArgument == null && valueParameter.getName() == JavaAnnotationResolver.DEFAULT_ANNOTATION_MEMBER_NAME) {
            javaAnnotationArgument = nameToArg[null]
        }

        resolveAnnotationArgument(javaAnnotationArgument)
    }

    override fun getValueArgument(valueParameterDescriptor: ValueParameterDescriptor) = _valueArguments(valueParameterDescriptor)

    private val _allValueArguments = c.storageManager.createLazyValue {
        val constructors = getAnnotationClass().getConstructors()
        if (constructors.isEmpty())
            mapOf<ValueParameterDescriptor, CompileTimeConstant<out Any?>>()
        else
            constructors.first().getValueParameters().keysToMapExceptNulls {
                vp -> getValueArgument(vp)
            }
    }

    override fun getAllValueArguments() = _allValueArguments()

    private fun getAnnotationClass() = getType().getConstructor().getDeclarationDescriptor() as ClassDescriptor

    private fun resolveAnnotationArgument(argument: JavaAnnotationArgument?): CompileTimeConstant<*>? {
        return when (argument) {
            is JavaLiteralAnnotationArgument -> JavaAnnotationArgumentResolver.resolveCompileTimeConstantValue(argument.getValue(), null)
            is JavaReferenceAnnotationArgument -> resolveFromReference(argument.resolve())
            is JavaArrayAnnotationArgument -> resolveFromArray(argument.getName() ?: DEFAULT_ANNOTATION_MEMBER_NAME, argument.getElements())
            is JavaAnnotationAsAnnotationArgument -> resolveFromAnnotation(argument.getAnnotation())
            is JavaClassObjectAnnotationArgument -> resolveFromJavaClassObjectType(argument.getReferencedType())
            else -> null
        }
    }

    private fun resolveFromAnnotation(javaAnnotation: JavaAnnotation): CompileTimeConstant<*>? {
        val fqName = javaAnnotation.getFqName()
        if (fqName == null) return null

        if (fqName.asString().startsWith("jet.runtime.typeinfo.") ||
            fqName == JETBRAINS_NOT_NULL_ANNOTATION ||
            fqName == JvmAnnotationNames.KOTLIN_CLASS ||
            fqName == JvmAnnotationNames.KOTLIN_PACKAGE) {
            return null
        }

        return AnnotationValue(LazyJavaAnnotationDescriptor(c, javaAnnotation))
    }

    private fun resolveFromArray(argumentName: Name, elements: List<JavaAnnotationArgument>): CompileTimeConstant<*>? {
        if (getType().isError()) return null

        val valueParameter = DescriptorResolverUtils.getAnnotationParameterByName(argumentName, getAnnotationClass())
        if (valueParameter == null) return null

        val values = elements.map {
            argument -> resolveAnnotationArgument(argument) ?: NullValue.NULL
        }
        return ArrayValue(values, valueParameter.getType())
    }

    private fun resolveFromReference(element: JavaElement?): CompileTimeConstant<*>? {
        if (element !is JavaField) return null

        if (!element.isEnumEntry()) return null

        val fqName = element.getContainingClass().getFqName()
        if (fqName == null) return null

        val enumClass = c.javaClassResolver.resolveClassByFqName(fqName)
        if (enumClass == null) return null

        for (variableDescriptor in DescriptorUtils.getEnumEntriesScope(enumClass).getProperties(element.getName())) {
            if (variableDescriptor.getReceiverParameter() == null) {
                return EnumValue(variableDescriptor as PropertyDescriptor)
            }

        }
        return null
    }

    private fun resolveFromJavaClassObjectType(javaType: JavaType): CompileTimeConstant<*>? {
        val `type` = c.typeResolver.transformJavaType(javaType, TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes())
        val jlClass = c.javaClassResolver.resolveClassByFqName(JavaAnnotationArgumentResolver.JL_CLASS_FQ_NAME)
        if (jlClass == null) return null

        val arguments = listOf(TypeProjectionImpl(`type`))

        val javaClassObjectType = object : LazyType(c.storageManager) {
            override fun computeTypeConstructor() = jlClass.getTypeConstructor()
            override fun computeArguments() = arguments
            override fun computeMemberScope() = jlClass.getMemberScope(arguments)
        }

        return JavaClassValue(javaClassObjectType)
    }


    override fun toString(): String {
        val annotationClassName = javaAnnotation.getFqName()?.shortName() ?: getType().getConstructor().getDeclarationDescriptor()?.getName()
        return annotationClassName?.asString() + DescriptorUtils.getSortedValueArguments(this, null);
    }
}