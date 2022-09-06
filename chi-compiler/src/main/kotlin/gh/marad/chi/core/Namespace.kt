package gh.marad.chi.core

import gh.marad.chi.core.astconverter.TypeResolver

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
                importTypeWithConstructors(
                    import.moduleName,
                    import.packageName,
                    entry.name,
                    entry.alias,
                    variantTypeDefinition
                )
            } else {
                importSymbol(import.moduleName, import.packageName, entry.name, entry.alias)
            }
        }

        if (import.packageAlias != null) {
            definePackageAlias(import.moduleName, import.packageName, import.packageAlias)
        }
    }

    fun addPreludeImport(preludeImport: PreludeImport) {
        val pkg = namespace.getOrCreatePackage(preludeImport.moduleName, preludeImport.packageName)
        val variantTypeDefinition = pkg.variantTypes.get(preludeImport.name)
        if (variantTypeDefinition != null) {
            importTypeWithConstructors(
                preludeImport.moduleName,
                preludeImport.packageName,
                preludeImport.name,
                preludeImport.alias,
                variantTypeDefinition
            )
        } else {
            importSymbol(preludeImport.moduleName, preludeImport.packageName, preludeImport.name, preludeImport.alias)
        }
    }

    fun lookupName(name: String): NameLookupResult? = nameLookupMap[name]
    fun lookupPackage(packageName: String): PackageLookupResult? = pkgLookupMap[packageName]
    fun lookupType(typeName: String): VariantTypeDefinition? = variantTypeLookupMap[typeName]

    data class NameLookupResult(val module: String, val pkg: String, val name: String)
    data class PackageLookupResult(val module: String, val pkg: String)

    private fun definePackageAlias(moduleName: String, packageName: String, alias: String) {
        pkgLookupMap[alias] = PackageLookupResult(moduleName, packageName)
    }

    private fun importTypeWithConstructors(
        moduleName: String,
        packageName: String,
        name: String,
        alias: String?,
        variantTypeDefinition: VariantTypeDefinition
    ) {
        variantTypeLookupMap[alias ?: name] = variantTypeDefinition
        variantTypeDefinition.variants.forEach { variant ->
            importSymbol(moduleName, packageName, variant.variantName)
        }
    }

    private fun importSymbol(moduleName: String, packageName: String, name: String, alias: String? = null) {
        nameLookupMap[alias ?: name] =
            NameLookupResult(moduleName, packageName, name)
    }
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
    val typeResolver: TypeResolver = TypeResolver(moduleName, packageName),
)

enum class SymbolScope { Local, Argument, Package }
data class SymbolInfo(val name: String, val type: Type, val scope: SymbolScope, val slot: Int, val mutable: Boolean)
data class CompilationScope(private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()
    val isTopLevel = parent == null

    fun addSymbol(name: String, type: Type, scope: SymbolScope, mutable: Boolean = false) {
        val existingType = getSymbolType(name)
        val finalType = if (type is FnType) {
            if (existingType != type) {
                when (existingType) {
                    is FnType ->
                        OverloadedFnType(setOf(existingType, type))
                    is OverloadedFnType ->
                        existingType.addFnType(type)
                    else ->
                        type
                }
            } else {
                existingType
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
    val baseType: VariantType,
    val variants: List<VariantType.Variant>
) {
    val simpleName get() = baseType.simpleName

    fun getWithSingleOrNoVariant() = baseType
    fun construct(variant: VariantType.Variant) = baseType.copy(variant = variant)
}

class VariantTypesDefinitions {
    private val types = mutableMapOf<String, VariantTypeDefinition>()

    fun defineType(type: VariantTypeDefinition) {
        types[type.simpleName] = type
    }

    fun get(simpleName: String): VariantTypeDefinition? = types[simpleName]
}