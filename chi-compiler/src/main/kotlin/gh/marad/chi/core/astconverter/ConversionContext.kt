package gh.marad.chi.core.astconverter

import gh.marad.chi.core.Expression
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.parser.readers.TypeRef

class ConversionContext(val namespace: GlobalCompilationNamespace) {
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

    //
    // Scoping
    //
    fun subScope() = CompilationScope(currentScope)

    fun <T> withNewScope(f: () -> T): T = withScope(subScope(), f)

    fun <T> withScope(scope: CompilationScope, f: () -> T): T {
        val previousScope = currentScope
        currentScope = scope
        try {
            return f()
        } finally {
            currentScope = previousScope
        }
    }

    //
    // Symbol lookup
    //
    data class SymbolLookupResult(
        val moduleName: String,
        val packageName: String,
        val scope: CompilationScope,
        val name: String
    )

    fun lookup(name: String): SymbolLookupResult {
        return sequenceOf(
            { // try looking in the scope
                currentScope.getSymbol(name)?.let {
                    SymbolLookupResult(currentModule, currentPackage, currentScope, name)
                }
            },
            { // then try imports
                imports.lookupName(name)?.let {
                    val scope = namespace.getOrCreatePackage(it.module, it.pkg).scope
                    SymbolLookupResult(it.module, it.pkg, scope, it.name)
                }
            },
            { // leap of faith to reading from current scope
                SymbolLookupResult(currentModule, currentPackage, currentScope, name)
            }
        ).map { it() }.filterNotNull().first()
    }

    //
    // Variant type lookup
    //
    data class TypeLookupResult(
        val moduleName: String,
        val packageName: String,
        val type: Type,
        val variants: List<VariantType.Variant>?,
    )

    fun lookupType(name: String): TypeLookupResult {
        return sequenceOf(
            {
                currentPackageDescriptor.typeRegistry.getTypeOrNull(name)?.let { type ->
                    val variants = currentPackageDescriptor.typeRegistry.getTypeVariants(name)
                    TypeLookupResult(currentModule, currentPackage, type, variants)
                }
            },
            {
                imports.getImportedType(name)?.let {
                    val pkgDesc = namespace.getOrCreatePackage(it.module, it.pkg)
                    pkgDesc.typeRegistry.getTypeOrNull(it.name)?.let { type ->
                        val variants = pkgDesc.typeRegistry.getTypeVariants(it.name)
                        TypeLookupResult(pkgDesc.moduleName, pkgDesc.packageName, type, variants)
                    }
                }
            }
        ).map { it() }.filterNotNull().firstOrNull() ?: TODO("Type $name not found!")
    }

    //
    // Generics and type resolving
    //
    fun <T> withTypeParameters(typeParameterNames: Set<String>, f: () -> T): T =
        namespace.typeResolver.withTypeParameters(typeParameterNames, f)

    fun resolveType(typeRef: TypeRef, typeParameterNames: Set<String> = emptySet()): Type {
        return namespace.typeResolver.resolve(typeRef, typeParameterNames) { typeName ->
            lookupType(typeName).type
        }
    }

    //
    // Temp Variables
    //
    private var tempVarNum = 0
    fun nextTempVarName() = "tempVar$${tempVarNum++}"

    //
    // Weave expression reading context
    //
    var currentWeaveInput: Expression? = null
        private set

    fun withWeaveInput(weaveInput: Expression, f: () -> Expression): Expression {
        val previous = currentWeaveInput
        currentWeaveInput = weaveInput
        val result = f()
        currentWeaveInput = previous
        return result
    }

    //
    // If reading context
    //

    data class IfReadingContext(
        val thenScope: CompilationScope,
        val elseScope: CompilationScope
    )

    var currentIfReadingContext: IfReadingContext? = null
        private set

    fun <T> withIfReadingContext(context: IfReadingContext, f: () -> T): T {
        val previous = currentIfReadingContext
        currentIfReadingContext = context
        val result = f()
        currentIfReadingContext = previous
        return result
    }
}
