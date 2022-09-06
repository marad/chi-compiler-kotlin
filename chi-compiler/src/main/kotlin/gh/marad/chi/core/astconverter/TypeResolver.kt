package gh.marad.chi.core.astconverter

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser2.*

class TypeResolver private constructor() {
    private val types: MutableMap<String, Type> = mutableMapOf(
        "any" to Type.any,
        "int" to Type.intType,
        "float" to Type.floatType,
        "unit" to Type.unit,
        "string" to Type.string,
        "bool" to Type.bool,
        "array" to Type.array(Type.typeParameter("T"))
    )
    private val variants = mutableMapOf<String, List<ParseVariantTypeDefinition.Constructor>>()

    private var contextParameterNames = emptySet<String>()
    fun <T> withTypeParameters(typeParameterNames: Set<String>, f: () -> T): T {
        val previous = contextParameterNames
        contextParameterNames = contextParameterNames + typeParameterNames
        val value = f()
        contextParameterNames = previous
        return value
    }

    fun resolve(
        ref: TypeRef,
        providedTypeParameterNames: Set<String>
    ): Type {
        val typeParameterNames = providedTypeParameterNames + contextParameterNames
        return when (ref) {
            is TypeNameRef -> {
                if (typeParameterNames.contains(ref.typeName)) {
                    Type.typeParameter(ref.typeName)
                } else if (variants.contains(ref.typeName)) {
                    val type = types[ref.typeName] as VariantType
                    val variantTypeParameters = type.genericTypeParameters.map { it.name }
                    type.withVariant(singleVariantOrNull(ref.typeName, typeParameterNames + variantTypeParameters))
                } else {
                    // TODO: sprawdź, że nazwa pod tym typem istnieje!!
                    types[ref.typeName] ?: TODO("Type ${ref.typeName} not found!")
                }
            }
            is FunctionTypeRef ->
                FnType(
                    genericTypeParameters = ref.typeParameters.filterIsInstance<TypeParameter>()
                        .map { Type.typeParameter(it.name) },
                    paramTypes = ref.argumentTypeRefs.map { resolve(it, typeParameterNames) },
                    returnType = resolve(ref.returnType, typeParameterNames)
                )
            is TypeConstructorRef -> {
                // TODO: sprawdź, że typ isTypeConstructor()
                // TODO: sprawdź, że ma tyle samo type parametrów co podane
                val allTypeParameterNames =
                    typeParameterNames + ref.typeParameters.filterIsInstance<TypeParameter>()
                        .map { it.name }.toSet()
                val type = resolve(ref.baseType, allTypeParameterNames)
                val parameterTypes = ref.typeParameters.map { resolve(it, allTypeParameterNames) }
                type.applyTypeParameters(parameterTypes)
            }
            is VariantNameRef -> {
                val variantType = resolve(ref.variantType, typeParameterNames) as VariantType
                variantType.withVariant(
                    VariantType.Variant(
                        variantName = ref.variantName,
                        fields = ref.variantFields.map {
                            VariantType.VariantField(
                                name = it.name,
                                type = resolve(it.typeRef, typeParameterNames)
                            )
                        }
                    )
                )
            }
            is TypeParameter -> Type.typeParameter(ref.name)
        }
    }

    fun addVariantType(moduleName: String, packageName: String, typeDefinition: ParseVariantTypeDefinition) {
        types[typeDefinition.typeName] = VariantType(
            moduleName,
            packageName,
            typeDefinition.typeName,
            typeDefinition.typeParameters.map { typeParam -> Type.typeParameter(typeParam.name) },
            emptyMap(),
            null,
        )

        variants[typeDefinition.typeName] = typeDefinition.variantConstructors
    }

    private fun singleVariantOrNull(typeName: String, typeParameterNames: Set<String>): VariantType.Variant? {
        return variants[typeName]?.singleOrNull()?.let {
            VariantType.Variant(
                variantName = it.name,
                fields = it.formalArguments.map { arg ->
                    VariantType.VariantField(arg.name, resolve(arg.typeRef, typeParameterNames))
                }
            )
        }
    }

    companion object {
        fun create(
            program: Program,
        ): TypeResolver {
            val moduleName = program.packageDefinition?.moduleName?.name ?: CompilationDefaults.defaultModule
            val packageName = program.packageDefinition?.packageName?.name ?: CompilationDefaults.defaultPacakge
            val typeResolver = TypeResolver()
            program.typeDefinitions.forEach { typeDefinition ->
                typeResolver.addVariantType(moduleName, packageName, typeDefinition)
            }
            return typeResolver
        }
    }
}

