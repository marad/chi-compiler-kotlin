package gh.marad.chi.core

class GlobalCompilationNamespace(private val prelude: List<PreludeImport> = emptyList()) {
    private val modules: MutableMap<String, ModuleDescriptor> = mutableMapOf()

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

class CompileTimeImports(private val namespace: GlobalCompilationNamespace) {
    private val nameLookupMap = mutableMapOf<String, NameLookupResult>()
    private val pkgLookupMap = mutableMapOf<String, PackageLookupResult>()
    private val variantTypeLookupMap = mutableMapOf<String, VariantTypeDefinition>()

    fun addImport(import: Import) {
        val pkg = namespace.getOrCreatePackage(import.moduleName, import.packageName)

        import.entries.forEach { entry ->
            val variantTypeDefinition = pkg.variantTypes.get(entry.name)
            if (variantTypeDefinition != null) {
                // import type and its constructors
                variantTypeLookupMap[entry.alias ?: entry.name] = variantTypeDefinition
                variantTypeDefinition.variants.forEach { variant ->
                    nameLookupMap[variant.variantName] =
                        NameLookupResult(import.moduleName, import.packageName, variant.variantName)
                }
            } else {
                // import regular function
                nameLookupMap[entry.alias ?: entry.name] =
                    NameLookupResult(import.moduleName, import.packageName, entry.name)
            }

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
    fun lookupType(typeName: String): VariantTypeDefinition? = variantTypeLookupMap[typeName]

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
    val scope: CompilationScope = CompilationScope(),
    val variantTypes: VariantTypesDefinitions = VariantTypesDefinitions(),
)

enum class SymbolScope { Local, Argument, Package }
data class SymbolInfo(val name: String, val type: Type, val scope: SymbolScope, val slot: Int, val mutable: Boolean)
data class CompilationScope(private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()
    val isTopLevel = parent == null

    fun addSymbol(name: String, type: Type, scope: SymbolScope, mutable: Boolean = false) {
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
        symbols[name] = SymbolInfo(name, finalType, scope, -1, mutable)
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

data class VariantTypeDefinition(
    val moduleName: String,
    val packageName: String,
    val simpleName: String,
    val genericTypeParameters: List<GenericTypeParameter>,
    val variants: List<VariantType.Variant>
) {
    fun getWithSingleOrNoVariant() =
        VariantType(moduleName, packageName, simpleName, genericTypeParameters, variants.singleOrNull())

    fun construct(variant: VariantType.Variant) =
        VariantType(moduleName, packageName, simpleName, genericTypeParameters, variant)
}

class VariantTypesDefinitions {
    private val types = mutableMapOf<String, VariantTypeDefinition>()

    fun defineType(type: VariantTypeDefinition) {
        types[type.simpleName] = type
    }

    fun get(simpleName: String): VariantTypeDefinition? = types[simpleName]
}