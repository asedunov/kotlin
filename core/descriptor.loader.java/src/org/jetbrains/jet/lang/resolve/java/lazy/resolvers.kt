/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.utils.toMap
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.jet.storage.LockBasedStorageManager
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver

trait LazyJavaClassResolver {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor?
}

trait TypeParameterResolver {
    class object {
        object EMPTY : TypeParameterResolver {
            override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? = null
        }
    }

    fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor?
}

class TypeParameterResolverImpl(
        _typeParameters: Collection<LazyJavaTypeParameterDescriptor>,
        private val parent: TypeParameterResolver = TypeParameterResolver.EMPTY
) : TypeParameterResolver {

    private val parameters = _typeParameters.toMap { p -> p.javaTypeParameter }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return parameters[javaTypeParameter] ?: parent.resolveTypeParameter(javaTypeParameter)
    }
}

class LazyJavaTypeParameterResolver(
        storageManager: LockBasedStorageManager,
        private val containingDeclaration: DeclarationDescriptor,
        private val _typeParameters: (JavaTypeParameter) -> Boolean,
        private val parent: TypeParameterResolver = TypeParameterResolver.EMPTY
) : TypeParameterResolver {

    private val resolve: (JavaTypeParameter) -> TypeParameterDescriptor? = storageManager.createMemoizedFunctionWithNullableValues {
                javaTypeParameter ->
                if (_typeParameters(javaTypeParameter))
                    LazyJavaTypeParameterDescriptor(
                            javaTypeParameter,
                            LazyJavaTypeResolver(storageManager, javaClassResolver, this),
                            storageManager,
                            containingDeclaration
                    )
                else null
            }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return resolve(javaTypeParameter) ?: parent.resolveTypeParameter(javaTypeParameter)
    }
}