package gh.marad.chi.core.parser

import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.GlobalCompilationNamespace

class ParsingContext(val namespace: GlobalCompilationNamespace) {
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
}
