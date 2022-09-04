package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser2.*

object FooBar {
    val basicTypes = mapOf(
        "int" to Type.intType,
        "float" to Type.floatType,
        "unit" to Type.unit,
        "string" to Type.string,
        "bool" to Type.bool,
        "array" to Type.array(Type.typeParameter("T"))
    )

    fun getFunctionDescriptors(program: Program): List<FunctionDescriptor> {
        val types = basicTypes + getDefinedTypes(program)
        val functionTypeRefs = getDefinedFunctionTypeRefs(program)

        return functionTypeRefs.map {
            val type = resolveType(it.type, types) as FnType
            FunctionDescriptor(it.name, type)
        }
    }

    fun getDefinedTypes(
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

    fun getDefinedFunctionTypeRefs(
        program: Program,
    ): List<FunctionDescriptorWithTypeRef> {
        val constructorFunctions = getTypesConstructors(program)
        val functions = program.functions
        return (constructorFunctions + functions).map { getFunctionTypeRef(it) }
    }

    fun resolveType(
        ref: TypeRef,
        types: Map<String, Type>,
        typeParameterNames: Set<String> = emptySet()
    ): Type =
        when (ref) {
            is TypeNameRef -> {
                if (typeParameterNames.contains(ref.typeName)) {
                    Type.typeParameter(ref.typeName)
                } else {
                    // TODO: sprawdź, że nazwa pod tym typem istnieje!!
                    types[ref.typeName] ?: TODO("Type ${ref.typeName} not found!")
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

    private fun getTypesConstructors(program: Program): List<ParseFuncWithName> =
        program.typeDefinitions.flatMap { typeDefinition -> createConstructorFunctions(typeDefinition) }

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

    private fun getFunctionTypeRef(it: ParseAst): FunctionDescriptorWithTypeRef {
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
                FunctionDescriptorWithTypeRef(it.name.name, typeRef)
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
                FunctionDescriptorWithTypeRef(it.name, typeRef)
            }

            else -> TODO("This is not a function declaration: $it")
        }
    }
}

data class FunctionDescriptor(val name: String, val type: FnType)
data class FunctionDescriptorWithTypeRef(val name: String, val type: TypeRef)

