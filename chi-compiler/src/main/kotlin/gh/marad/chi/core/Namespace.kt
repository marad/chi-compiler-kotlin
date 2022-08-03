package gh.marad.chi.core

class GlobalCompilationNamespace(private val prelude: List<PreludeImport> = emptyList()) {
    private val modules: MutableMap<String, ModuleDescriptor> = mutableMapOf()

    fun getDefaultScope() =
        getOrCreatePackageScope(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge)

    fun getOrCreatePackageScope(moduleName: String, packageName: String): CompilationScope =
        getOrCreateModule(moduleName)
            .getOrCreatePackageScope(packageName)

    fun setPackageScope(moduleName: String, packageName: String, scope: CompilationScope) {
        getOrCreateModule(moduleName).setPackageScope(packageName, scope)
    }

    fun createCompileTimeImports(): CompileTimeImports =
        CompileTimeImports().also {
            prelude.forEach(it::addPreludeImport)
        }

    private fun getOrCreateModule(moduleName: String) = modules.getOrPut(moduleName) { ModuleDescriptor(moduleName) }
}

class CompileTimeImports {
    private val nameLookupMap = mutableMapOf<String, NameLookupResult>()
    private val pkgLookupMap = mutableMapOf<String, PackageLookupResult>()
    fun addImport(import: Import) {
        import.entries.forEach { entry ->
            nameLookupMap[entry.alias ?: entry.name] =
                NameLookupResult(import.moduleName, import.packageName, entry.name)
        }

        if (import.packageAlias != null) {
            pkgLookupMap[import.packageAlias] = PackageLookupResult(import.moduleName, import.packageName)
        }
    }

    fun addPreludeImport(preludeImport: PreludeImport) {
        nameLookupMap[preludeImport.alias ?: preludeImport.name] =
            NameLookupResult(preludeImport.moduleName, preludeImport.packageName, preludeImport.name)
    }

    fun lookupName(name: String): NameLookupResult? = nameLookupMap[name]
    fun lookupPackage(packageName: String): PackageLookupResult? = pkgLookupMap[packageName]

    data class NameLookupResult(val module: String, val pkg: String, val name: String)
    data class PackageLookupResult(val module: String, val pkg: String)
}


data class PreludeImport(
    val moduleName: String,
    val packageName: String,
    val name: String,
    val alias: String?
)

class ModuleDescriptor(
    val moduleName: String,
    private val packageScopes: MutableMap<String, CompilationScope> = mutableMapOf()
) {
    fun getOrCreatePackageScope(packageName: String): CompilationScope =
        packageScopes.getOrPut(packageName) { CompilationScope() }

    fun setPackageScope(packageName: String, scope: CompilationScope) = packageScopes.put(packageName, scope)
}

enum class SymbolScope { Local, Argument, Package }
data class SymbolInfo(val name: String, val type: Type, val scope: SymbolScope, val slot: Int)
data class CompilationScope(private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()
    val isTopLevel = parent == null

    fun addSymbol(name: String, type: Type, scope: SymbolScope) {
        val existingType = getSymbolType(name)
        val finalType = if (type is FnType) {
            when (existingType) {
                is FnType ->
                    OverloadedFnType(setOf(existingType, type))
                is OverloadedFnType ->
                    existingType.addFnType(type)
                else ->
                    type
            }
        } else {
            type
        }
        symbols[name] = SymbolInfo(name, finalType, scope, -1)
    }

    fun getSymbolType(name: String): Type? = symbols[name]?.type ?: parent?.getSymbolType(name)

    fun getSymbol(name: String): SymbolInfo? = symbols[name] ?: parent?.getSymbol(name)

    fun containsSymbol(name: String): Boolean = getSymbolType(name) != null

    fun containsDirectly(name: String) = symbols.contains(name)

    fun updateSlot(name: String, slot: Int) {
        symbols.compute(name) { _, symbol ->
            symbol?.copy(slot = slot)
        }
    }
}
