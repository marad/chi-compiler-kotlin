package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.FnType
import gh.marad.chi.core.parser2.*

object FooBar {

    fun getFunctionDescriptors(program: Program, ctx: ConversionContext): List<FunctionDescriptor> {
        val functionTypeRefs = getDefinedFunctionTypeRefs(program)

        return functionTypeRefs.map {
            val type = ctx.resolveType(it.type) as FnType
            FunctionDescriptor(it.name, type)
        }
    }

    fun getDefinedFunctionTypeRefs(
        program: Program,
    ): List<FunctionDescriptorWithTypeRef> {
        val constructorFunctions = getTypesConstructors(program)
        val functions = program.functions
        return (constructorFunctions + functions).map { getFunctionTypeRef(it) }
    }

    private fun getTypesConstructors(program: Program): List<ParseFuncWithName> =
        program.typeDefinitions.flatMap { typeDefinition -> createConstructorFunctions(typeDefinition) }

    private fun createConstructorFunctions(typeDef: ParseVariantTypeDefinition): List<ParseFuncWithName> =
        typeDef.variantConstructors.map {
            ParseFuncWithName(
                name = it.name,
                typeParameters = typeDef.typeParameters,
                formalArguments = it.formalArguments,
                returnTypeRef = VariantNameRef(
                    variantType = TypeNameRef(typeDef.typeName, null),
                    variantName = it.name,
                    variantFields = it.formalArguments,
                    section = null
                ),
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

