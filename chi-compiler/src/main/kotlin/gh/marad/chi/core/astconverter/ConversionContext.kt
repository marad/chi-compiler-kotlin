package gh.marad.chi.core.astconverter

import gh.marad.chi.core.Expression
import gh.marad.chi.core.Type
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
        return sequenceOf(
            { // try looking in the scope
                currentScope.getSymbol(name)?.let {
                    LookupResult(currentModule, currentPackage, currentScope, name)
                }
            },
            { // then try imports
                imports.lookupName(name)?.let {
                    val scope = namespace.getOrCreatePackage(it.module, it.pkg).scope
                    LookupResult(it.module, it.pkg, scope, it.name)
                }
            },
            { // leap of faith to reading from current scope
                LookupResult(currentModule, currentPackage, currentScope, name)
            }
        ).map { it() }.filterNotNull().first()
    }

    var currentWeaveInput: Expression? = null
        private set

    fun withWeaveInput(weaveInput: Expression, f: () -> Expression): Expression {
        val previous = currentWeaveInput
        currentWeaveInput = weaveInput
        val result = f()
        currentWeaveInput = previous
        return result
    }


    fun <T> withTypeParameters(typeParameterNames: Set<String>, f: () -> T): T =
        namespace.typeResolver.withTypeParameters(typeParameterNames, f)

    fun resolveType(typeRef: TypeRef, typeParameterNames: Set<String> = emptySet()): Type {
        return namespace.typeResolver.resolve(typeRef, typeParameterNames) { typeName ->
            val typePkg = imports.getImportedType(typeName)
            if (typePkg != null) {
                namespace.getOrCreatePackage(typePkg.module, typePkg.pkg)
                    .typeRegistry.getType(typePkg.name, this::resolveType)
            } else {
                currentPackageDescriptor
                    .typeRegistry.getType(typeName, this::resolveType)
            }
        }
    }
}
