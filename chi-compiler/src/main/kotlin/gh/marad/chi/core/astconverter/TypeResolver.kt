package gh.marad.chi.core.astconverter

import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser.*

class TypeRegistry {
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

    fun getType(name: String, resolveTypeRef: (TypeRef, typeParameterNames: Set<String>) -> Type): Type {
        return if (variants.contains(name)) {
            val type = types[name] as VariantType
            val variantTypeParameters = type.genericTypeParameters.map { it.name }.toSet()
            type.withVariant(singleVariantOrNull(name) { resolveTypeRef(it, variantTypeParameters) })
        } else {
            // TODO: sprawdź, że nazwa pod tym typem istnieje!!
            types[name] ?: TODO("Type $name not found!")
        }
    }

    fun getVariantTypeConstructors(
        variantName: String
    ): List<ParseVariantTypeDefinition.Constructor>? =
        variants[variantName]

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

    private fun singleVariantOrNull(
        typeName: String,
        resolveTypeRef: (TypeRef) -> Type,
    ): VariantType.Variant? {
        return variants[typeName]?.singleOrNull()?.let {
            VariantType.Variant(
                variantName = it.name,
                fields = it.formalArguments.map { arg ->
                    VariantType.VariantField(arg.name, resolveTypeRef(arg.typeRef))
                }
            )
        }
    }
}

class TypeResolver {
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
        providedTypeParameterNames: Set<String>,
        getTypeByName: (String) -> Type
    ): Type {
        val typeParameterNames = providedTypeParameterNames + contextParameterNames
        return when (ref) {
            is TypeNameRef -> {
                if (typeParameterNames.contains(ref.typeName)) {
                    Type.typeParameter(ref.typeName)
                } else {
                    getTypeByName(ref.typeName)
                }
            }
            is FunctionTypeRef ->
                FnType(
                    genericTypeParameters = ref.typeParameters.filterIsInstance<TypeParameter>()
                        .map { Type.typeParameter(it.name) },
                    paramTypes = ref.argumentTypeRefs.map { resolve(it, typeParameterNames, getTypeByName) },
                    returnType = resolve(ref.returnType, typeParameterNames, getTypeByName)
                )
            is TypeConstructorRef -> {
                // TODO: sprawdź, że typ isTypeConstructor()
                // TODO: sprawdź, że ma tyle samo type parametrów co podane
                val allTypeParameterNames =
                    typeParameterNames + ref.typeParameters.filterIsInstance<TypeParameter>()
                        .map { it.name }.toSet()
                val type = resolve(ref.baseType, allTypeParameterNames, getTypeByName)
                val parameterTypes = ref.typeParameters.map { resolve(it, allTypeParameterNames, getTypeByName) }
                type.applyTypeParameters(parameterTypes)
            }
            is VariantNameRef -> {
                val variantType = resolve(ref.variantType, typeParameterNames, getTypeByName) as VariantType
                variantType.withVariant(
                    VariantType.Variant(
                        variantName = ref.variantName,
                        fields = ref.variantFields.map {
                            VariantType.VariantField(
                                name = it.name,
                                type = resolve(it.typeRef, typeParameterNames, getTypeByName)
                            )
                        }
                    )
                )
            }
            is TypeParameter -> Type.typeParameter(ref.name)
        }
    }

}

