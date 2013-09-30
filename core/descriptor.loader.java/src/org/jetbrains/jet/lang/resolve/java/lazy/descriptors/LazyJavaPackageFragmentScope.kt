package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections
import org.jetbrains.jet.lang.resolve.name.FqName

public class LazyJavaPackageFragmentScope(
        containingDeclaration: NamespaceDescriptor, storageManager: StorageManager, finder: JavaClassFinder
) : LazyJavaMemberScope(containingDeclaration, storageManager, finder) {
    
    private val fqName = DescriptorUtils.getFQName(containingDeclaration).toSafe()
    private val classes = storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor>(
            {
                name ->
                val fqName = fqName.child(name!!)
                val javaClass = finder.findClass(fqName)
                if (javaClass == null)
                    null
                else
                    LazyJavaClassDescriptor(storageManager, containingDeclaration, fqName, javaClass)
            },
            StorageManager.ReferenceKind.STRONG)

    override fun getAllClassNames(): Collection<Name> {
        val javaPackage = finder.findPackage(fqName)
        assert(javaPackage != null) { "Package not found:  $fqName" }
        return javaPackage!!.getClasses().map { c -> c.getName() }
    }

    override fun getAllPropertyNames() = Collections.emptyList<Name>()
    override fun getAllFunctionNames() = Collections.emptyList<Name>()
    
    override fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>) {
        // no extra descriptors
    }

    override fun getClassifier(name: Name): ClassifierDescriptor {
        throw UnsupportedOperationException()
    }

    override fun getProperties(name: Name): MutableCollection<VariableDescriptor> {
        throw UnsupportedOperationException()
    }

    override fun getFunctions(name: Name): MutableCollection<FunctionDescriptor> {
        throw UnsupportedOperationException()
    }

    override fun getImplicitReceiversHierarchy(): MutableList<ReceiverParameterDescriptor> {
        throw UnsupportedOperationException()
    }

}
