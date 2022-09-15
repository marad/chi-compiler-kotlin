package gh.marad.chi.core.namespace

import gh.marad.chi.core.Import

class CompileTimeImports(private val namespace: GlobalCompilationNamespace) {
    private val nameLookupMap = mutableMapOf<String, NameLookupResult>()
    private val pkgLookupMap = mutableMapOf<String, PackageLookupResult>()
    private val importedTypes = mutableMapOf<String, NameLookupResult>()
    private val variantToTypeNames = mutableMapOf<String, NameLookupResult>()

    fun getImportedType(typeName: String) = importedTypes[typeName]
    fun getImportedTypeForVariantName(variantName: String) = variantToTypeNames[variantName]

    fun addImport(import: Import) {
        val pkg = namespace.getOrCreatePackage(import.moduleName, import.packageName)

        import.entries.forEach { entry ->
            val variants = pkg.typeRegistry.getTypeVariants(entry.name)
            if (variants != null) {
                val result = NameLookupResult(import.moduleName, import.packageName, entry.name)
                importedTypes[entry.alias ?: entry.name] = result
                variants.forEach {
                    importSymbol(
                        import.moduleName,
                        import.packageName,
                        it.variantName,
                        entry.alias
                    )
                    variantToTypeNames[it.variantName] = result
                }
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
        val variants = pkg.typeRegistry.getTypeVariants(preludeImport.name)
        if (variants != null) {
            val result = NameLookupResult(preludeImport.moduleName, preludeImport.packageName, preludeImport.name)
            importedTypes[preludeImport.alias ?: preludeImport.name] = result

            variants.forEach {
                importSymbol(
                    preludeImport.moduleName,
                    preludeImport.packageName,
                    it.variantName,
                )
                variantToTypeNames[it.variantName] = result
            }
        } else {
            importSymbol(preludeImport.moduleName, preludeImport.packageName, preludeImport.name, preludeImport.alias)
        }
    }

    fun lookupName(name: String): NameLookupResult? = nameLookupMap[name]
    fun lookupPackage(packageName: String): PackageLookupResult? = pkgLookupMap[packageName]

    data class NameLookupResult(val module: String, val pkg: String, val name: String)
    data class PackageLookupResult(val module: String, val pkg: String)

    private fun definePackageAlias(moduleName: String, packageName: String, alias: String) {
        pkgLookupMap[alias] = PackageLookupResult(moduleName, packageName)
    }

    private fun importSymbol(moduleName: String, packageName: String, name: String, alias: String? = null) {
        nameLookupMap[alias ?: name] =
            NameLookupResult(moduleName, packageName, name)
    }
}

