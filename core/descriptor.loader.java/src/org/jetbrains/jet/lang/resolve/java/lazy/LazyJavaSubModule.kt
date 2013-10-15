package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.jet.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder

public open class LazyJavaSubModule(
        c: LazyJavaResolverContext,
        module: ModuleDescriptor,
        finder: JavaClassFinder
) {
    public val packageFragments: MemoizedFunctionToNullable<FqName, NamespaceDescriptor>
            = c.storageManager.createMemoizedFunctionWithNullableValues {fqName -> LazyJavaPackageFragment(c, module, finder, fqName)}
}