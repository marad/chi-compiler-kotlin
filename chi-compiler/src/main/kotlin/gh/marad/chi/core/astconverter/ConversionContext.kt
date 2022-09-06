package gh.marad.chi.core.astconverter

import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.GlobalCompilationNamespace
import gh.marad.chi.core.Type
import gh.marad.chi.core.parser2.TypeRef

// TODO: types powinno być generowane przez packageDescriptor, ten powinien mieć już definicje
// TODO: trzeba to usunąć z FooBar.getDefinedTypes
class ConversionContext(val namespace: GlobalCompilationNamespace, private val typeResolver: TypeResolver) {
    val imports = namespace.createCompileTimeImports()
    var currentPackageDescriptor = namespace.getDefaultPackage()
        private set
    var currentScope = currentPackageDescriptor.scope
        private set

    val currentModule: String get() = currentPackageDescriptor.moduleName
    val currentPackage: String get() = currentPackageDescriptor.packageName

    fun changeCurrentPackage(moduleName: String, packageName: String) {
        currentPackageDescriptor = namespace.getOrCreatePackage(moduleName, packageName)
        currentScope = currentPackageDescriptor.scope
    }

    fun <T> withNewScope(f: () -> T): T {
        val parentScope = currentScope
        currentScope = CompilationScope(parentScope)
        try {
            return f()
        } finally {
            currentScope = parentScope
        }
    }

    data class LookupResult(
        val moduleName: String,
        val packageName: String,
        val scope: CompilationScope,
        val name: String
    )

    fun lookup(name: String): LookupResult {
        val imported = imports.lookupName(name)
        return if (imported != null) {
            LookupResult(
                imported.module,
                imported.pkg,
                namespace.getOrCreatePackage(imported.module, imported.pkg).scope,
                imported.name
            )
        } else {
            LookupResult(currentModule, currentPackage, currentScope, name)
        }
    }

    fun <T> withTypeParameters(typeParameterNames: Set<String>, f: () -> T): T =
        typeResolver.withTypeParameters(typeParameterNames, f)

    fun resolveType(typeRef: TypeRef, typeParameterNames: Set<String> = emptySet()): Type =
        typeResolver.resolve(typeRef, typeParameterNames)
}
