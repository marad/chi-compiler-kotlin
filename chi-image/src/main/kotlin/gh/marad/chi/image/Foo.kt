package gh.marad.chi.image

import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.namespace.*
import gh.marad.chi.core.parser.ChiSource

interface Code : java.io.Serializable

data class Symbol(
    val public: Boolean,
    val mutable: Boolean,
    val name: String,
    val code: Code,
    val type: Type,
    val sourceSection: ChiSource.Section
)

data class VariantTypeDefinition(
    val name: String,
    val type: VariantType,
    val variants: List<VariantType.Variant>
)

data class Package(
    val name: String,
    val source: ChiSource,
    val symbols: MutableMap<String, Symbol>,
    val types: MutableMap<String, VariantTypeDefinition>
) {
    fun createPackageDescriptor(moduleName: String): PackageDescriptor {
        return PackageDescriptor(
            moduleName, packageName = name,
            scope = createCompilationScope(),
            typeRegistry = createTypeRegistry()
        )
    }

    private fun createCompilationScope(): CompilationScope {
        val scope = CompilationScope(ScopeType.Package)
        symbols.values.forEach {
            scope.addSymbol(
                name = it.name,
                type = it.type,
                SymbolType.Local,
                public = it.public,
                mutable = it.mutable
            )
        }
        return scope
    }

    private fun createTypeRegistry(): TypeRegistry {
        val typeRegistry = TypeRegistry()
        types.values.forEach {
            typeRegistry.defineVariantType(
                it.type,
                it.variants
            )
        }
        return typeRegistry
    }
}

data class Module(
    val name: String,
    val packages: MutableMap<String, Package>
) {
    fun createModuleDescriptor(): ModuleDescriptor {
        return ModuleDescriptor(
            moduleName = name,
            packageDescriptors = packages.values
                .associate { it.name to it.createPackageDescriptor(name) }
                .toMutableMap()
        )
    }
}

class Image {
    val modules: Map<String, Module> = mutableMapOf()

    fun createNamespace(): GlobalCompilationNamespace {
        return GlobalCompilationNamespace(
            prelude = emptyList(), // TODO
            modules = modules.values.associate { it.name to it.createModuleDescriptor() }
                .toMutableMap()
        )
    }
}

// jak wygenerować global compilation namespace?
// - dla każdego pakietu stworzyć CompilationScope (zawiera symbole) \
//   i TypeResolver (zawiera definicje typów)

fun main() {
    GlobalCompilationNamespace()
}