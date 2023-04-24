package gh.marad.chi.core.namespace

import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType

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
    private val variants = mutableMapOf<String, List<VariantType.Variant>>()
    private val typeByVariantName = mutableMapOf<String, VariantType>()

    fun getTypeOrNull(name: String): Type? = types[name]

    fun getTypeVariants(variantName: String): List<VariantType.Variant>? = variants[variantName]

    fun getTypeByVariantName(variantName: String): VariantType? = typeByVariantName[variantName]?.let {
        it.withVariant(variants[it.simpleName]?.find { variant -> variant.variantName == variantName })
    }

    fun defineVariantType(
        type: VariantType,
        variants: List<VariantType.Variant>
    ) {
        this.types[type.simpleName] = type
        this.variants[type.simpleName] = variants
        variants.forEach { typeByVariantName[it.variantName] = type }
    }
}

