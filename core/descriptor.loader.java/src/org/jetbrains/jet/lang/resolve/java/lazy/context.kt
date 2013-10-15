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

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter

open class LazyJavaResolverContext(
        val storageManager: StorageManager,
        val javaClassResolver: LazyJavaClassResolver
)

fun LazyJavaResolverContext.withTypes(
        typeParameterResolver: TypeParameterResolver
)  =  LazyJavaResolverContextWithTypes(
        storageManager,
        javaClassResolver,
        LazyJavaTypeResolver(this, typeParameterResolver),
        typeParameterResolver)

class LazyJavaResolverContextWithTypes(
        storageManager: StorageManager,
        javaClassResolver: LazyJavaClassResolver,
        val typeResolver: LazyJavaTypeResolver,
        val typeParameterResolver: TypeParameterResolver
) : LazyJavaResolverContext(storageManager, javaClassResolver)

fun LazyJavaResolverContextWithTypes.child(
        containingDeclaration: DeclarationDescriptor,
        typeParameters: Set<JavaTypeParameter>
): LazyJavaResolverContextWithTypes = this.withTypes(LazyJavaTypeParameterResolver(this, containingDeclaration, typeParameters))