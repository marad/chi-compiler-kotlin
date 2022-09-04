package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser2.*

data class SymbolTable(val a: Int) {
    companion object {
        fun generate(program: Program): SymbolTable {

            val symbolTypeRefs = mutableMapOf<SymbolDesc, TypeRef>()
            val types = mutableMapOf(
                "int" to Type.intType,
                "float" to Type.floatType,
                "unit" to Type.unit,
                "string" to Type.string,
                "bool" to Type.bool
            )

            defineTypes(program, symbolTypeRefs, types)
            defineFunctions(program.functions, symbolTypeRefs)

            symbolTypeRefs.forEach(::println)
            types.forEach(::println)

            symbolTypeRefs.forEach {
                when (val ref = it.value) {
                    is TypeNameRef -> types[ref.typeName]
                    is FunctionTypeRef -> TODO()
                    is TypeConstructorRef -> TODO()
                    is TypeParameter -> TODO()
                }
            }
            return SymbolTable(0)
        }

        private fun defineTypes(
            program: Program,
            symbolTypeRefs: MutableMap<SymbolDesc, TypeRef>,
            types: MutableMap<String, Type>
        ) {
            val moduleName = program.packageDefinition?.moduleName?.name ?: CompilationDefaults.defaultModule
            val packageName = program.packageDefinition?.packageName?.name ?: CompilationDefaults.defaultPacakge
            program.typeDefinitions.forEach {
                it.variantConstructors.forEach { constructor ->
                    val typeNameRef = TypeNameRef(it.typeName, null)
                    val constructorReturnType = if (it.typeParameters.isNotEmpty()) {
                        TypeConstructorRef(typeNameRef, it.typeParameters, null)
                    } else {
                        typeNameRef
                    }
                    val argumentTypeRefs = constructor.formalArguments.map { arg -> arg.typeRef }
                    val typeRef = FunctionTypeRef(
                        argumentTypeRefs,
                        constructorReturnType,
                        null
                    )
                    val key = SymbolDesc(constructor.name, argumentTypeRefs)
                    symbolTypeRefs[key] = typeRef
                }

                types[it.typeName] = VariantType(
                    moduleName,
                    packageName,
                    it.typeName,
                    it.typeParameters.map { Type.typeParameter(it.name) },
                    emptyMap(),
                    null
                )
            }
        }

        private fun defineFunctions(
            functions: List<ParseAst>,
            symbolTypeRefs: MutableMap<SymbolDesc, TypeRef>
        ) {
            functions.forEach {
                when (it) {
                    is ParseNameDeclaration -> {
                        val func = it.value as ParseFunc
                        val typeRef = it.typeRef ?: func.returnTypeRef
                        val key = SymbolDesc(it.name.name, func.formalArguments.map { arg -> arg.typeRef })
                        symbolTypeRefs[key] = typeRef
                    }

                    is ParseFuncWithName -> {
                        val argumentTypeRefs = it.formalArguments.map { it.typeRef }
                        val typeRef = FunctionTypeRef(
                            argumentTypeRefs,
                            it.returnTypeRef ?: TypeNameRef("unit", null),
                            null
                        )
                        val key = SymbolDesc(it.name, argumentTypeRefs)
                        symbolTypeRefs[key] = typeRef
                    }

                    else -> TODO("This is not a function declaration: $it")
                }
            }
        }
    }
}

data class SymbolDesc(val name: String, val argTypes: List<TypeRef>)

