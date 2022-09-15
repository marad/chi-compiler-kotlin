package gh.marad.chi.core.namespace

import gh.marad.chi.core.CompilationDefaults

class GlobalCompilationNamespace(private val prelude: List<PreludeImport> = emptyList()) {
    private val modules: MutableMap<String, ModuleDescriptor> = mutableMapOf()
    val typeResolver = TypeResolver()

    init {
        getDefaultPackage().typeRegistry
    }

    fun setPackageScope(moduleName: String, packageName: String, scope: CompilationScope) {
        getOrCreateModule(moduleName).setPackageScope(packageName, scope)
    }

    fun createCompileTimeImports(): CompileTimeImports =
        CompileTimeImports(this).also {
            prelude.forEach(it::addPreludeImport)
        }

    fun getDefaultPackage() =
        getOrCreatePackage(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge)

    fun getOrCreatePackage(moduleName: String, packageName: String): PackageDescriptor =
        getOrCreateModule(moduleName).getOrCreatePackage(packageName)

    private fun getOrCreateModule(moduleName: String) = modules.getOrPut(moduleName) { ModuleDescriptor(moduleName) }
}


data class PreludeImport(
    val moduleName: String,
    val packageName: String,
    val name: String,
    val alias: String?
)

class ModuleDescriptor(
    val moduleName: String,
    private val packageDescriptors: MutableMap<String, PackageDescriptor> = mutableMapOf()
) {
    fun getOrCreatePackage(packageName: String): PackageDescriptor =
        packageDescriptors.getOrPut(packageName) {
            PackageDescriptor(moduleName, packageName)
        }

    fun setPackageScope(packageName: String, scope: CompilationScope) =
        packageDescriptors.put(packageName, getOrCreatePackage(packageName).copy(scope = scope))
}

data class PackageDescriptor(
    val moduleName: String,
    val packageName: String,
    val scope: CompilationScope = CompilationScope(ScopeType.Package),
    val typeRegistry: TypeRegistry = TypeRegistry(),
)

