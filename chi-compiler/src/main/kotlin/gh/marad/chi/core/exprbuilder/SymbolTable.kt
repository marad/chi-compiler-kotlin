package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser2.*

data class SymbolTable(
    val a: Int
) {
    companion object {
        val basicTypes = mapOf(
            "int" to Type.intType,
            "float" to Type.floatType,
            "unit" to Type.unit,
            "string" to Type.string,
            "bool" to Type.bool,
            "array" to Type.array(Type.typeParameter("T"))
        )

        fun generate(program: Program): Map<SymbolKey, FnType> {
            val types = basicTypes + getDefinedTypes(program)
            val functionTypeRefs = getDefinedFunctionTypeRefs(program)

            val result = functionTypeRefs.map {
                val type = resolveType(it.value, types) as FnType
                SymbolKey(it.key.name, type.paramTypes) to type
            }.toMap()

            result.forEach {
                println("- ${it.key.name} : ${it.value}")
            }
            return result
        }

        private fun resolveType(
            ref: TypeRef,
            types: Map<String, Type>,
            typeParameterNames: Set<String> = emptySet()
        ): Type =
            when (ref) {
                is TypeNameRef -> {
                    if (typeParameterNames.contains(ref.typeName)) {
                        Type.typeParameter(ref.typeName)
                    } else {
                        types[ref.typeName]!! // TODO: sprawdź, że nazwa pod tym typem istnieje!!
                    }
                }
                is FunctionTypeRef ->
                    FnType(
                        genericTypeParameters = ref.typeParameters.filterIsInstance<TypeParameter>()
                            .map { Type.typeParameter(it.name) },
                        paramTypes = ref.argumentTypeRefs.map { resolveType(it, types, typeParameterNames) },
                        returnType = resolveType(ref.returnType, types, typeParameterNames)
                    )
                is TypeConstructorRef -> {
                    // TODO: sprawdź, że typ isTypeConstructor()
                    // TODO: sprawdź, że ma tyle samo type parametrów co podane
                    val allTypeParameterNames =
                        typeParameterNames + ref.typeParameters.filterIsInstance<TypeParameter>()
                            .map { it.name }.toSet()
                    val type = resolveType(ref.baseType, types, allTypeParameterNames)
                    val parameterTypes = ref.typeParameters.map { resolveType(it, types, allTypeParameterNames) }
                    type.applyTypeParameters(parameterTypes)
                }
                is TypeParameter -> Type.typeParameter(ref.name)
            }

        private fun getDefinedTypes(
            program: Program,
        ): Map<String, Type> {
            val types = mutableMapOf<String, Type>()
            val moduleName = program.packageDefinition?.moduleName?.name ?: CompilationDefaults.defaultModule
            val packageName = program.packageDefinition?.packageName?.name ?: CompilationDefaults.defaultPacakge
            program.typeDefinitions.forEach { typeDefinition ->
                types[typeDefinition.typeName] = VariantType(
                    moduleName,
                    packageName,
                    typeDefinition.typeName,
                    typeDefinition.typeParameters.map { typeParam -> Type.typeParameter(typeParam.name) },
                    emptyMap(),
                    null
                )
            }
            return types
        }

        private fun getTypesConstructors(program: Program): List<ParseFuncWithName> =
            program.typeDefinitions.flatMap { createConstructorFunctions(it) }

        private fun createConstructorFunctions(typeDef: ParseVariantTypeDefinition): List<ParseFuncWithName> =
            typeDef.variantConstructors.map {
                ParseFuncWithName(
                    name = it.name,
                    typeParameters = typeDef.typeParameters,
                    formalArguments = it.formalArguments,
                    returnTypeRef = TypeNameRef(typeDef.typeName, null),
                    body = ParseBlock(emptyList(), null),
                    section = null
                )
            }

        private fun getDefinedFunctionTypeRefs(
            program: Program,
        ): Map<SymbolDesc, TypeRef> {
            val constructorFunctions = getTypesConstructors(program)
            val functions = program.functions
            return (constructorFunctions + functions).associate { getFunctionTypeRef(it) }
        }

        private fun getFunctionTypeRef(it: ParseAst): Pair<SymbolDesc, TypeRef> {
            return when (it) {
                is ParseNameDeclaration -> {
                    val func = it.value as ParseFunc
                    val funcTypeRef = FunctionTypeRef(
                        typeParameters = emptyList(),
                        argumentTypeRefs = func.formalArguments.map { it.typeRef },
                        func.returnTypeRef,
                        null
                    )
                    val typeRef = it.typeRef ?: funcTypeRef

                    val key = SymbolDesc(it.name.name, func.formalArguments.map { arg -> arg.typeRef })
                    key to typeRef
                }

                is ParseFuncWithName -> {
                    val argumentTypeRefs = it.formalArguments.map { it.typeRef }
                    val functionTypeRef = FunctionTypeRef(
                        it.typeParameters,
                        argumentTypeRefs,
                        it.returnTypeRef ?: TypeNameRef("unit", null),
                        null
                    )
                    val typeRef = if (it.typeParameters.isEmpty()) {
                        functionTypeRef
                    } else {
                        TypeConstructorRef(functionTypeRef, it.typeParameters, null)
                    }

                    val key = SymbolDesc(it.name, argumentTypeRefs)
                    key to typeRef
                }

                else -> TODO("This is not a function declaration: $it")
            }
        }
    }
}

data class SymbolDesc(val name: String, val argTypes: List<TypeRef>)
data class SymbolKey(val name: String, val argTypes: List<Type>)

