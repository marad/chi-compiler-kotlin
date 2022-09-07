package gh.marad.chi.core.namespace

import gh.marad.chi.core.Import

class CompileTimeImports(private val namespace: GlobalCompilationNamespace) {
    private val nameLookupMap = mutableMapOf<String, NameLookupResult>()
    private val pkgLookupMap = mutableMapOf<String, PackageLookupResult>()
    private val importedTypes = mutableMapOf<String, NameLookupResult>()

    fun getImportedType(typeName: String) = importedTypes[typeName]

    fun addImport(import: Import) {
        val pkg = namespace.getOrCreatePackage(import.moduleName, import.packageName)

        import.entries.forEach { entry ->
            val constructors = pkg.typeRegistry.getVariantTypeConstructors(entry.name)
            if (constructors != null) {
                importedTypes[entry.alias ?: entry.name] =
                    NameLookupResult(import.moduleName, import.packageName, entry.name)
                constructors.forEach {
                    importSymbol(
                        import.moduleName,
                        import.packageName,
                        it.name,
                        entry.alias
                    )
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
        val constructors = pkg.typeRegistry.getVariantTypeConstructors(preludeImport.name)
        if (constructors != null) {
            importedTypes[preludeImport.alias ?: preludeImport.name] =
                NameLookupResult(preludeImport.moduleName, preludeImport.packageName, preludeImport.name)
            constructors.forEach {
                importSymbol(
                    preludeImport.moduleName,
                    preludeImport.packageName,
                    it.name,
                )
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

