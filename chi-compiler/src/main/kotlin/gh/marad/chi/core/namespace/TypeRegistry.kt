package gh.marad.chi.core.namespace

import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser.readers.ParseVariantTypeDefinition
import gh.marad.chi.core.parser.readers.TypeRef

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

