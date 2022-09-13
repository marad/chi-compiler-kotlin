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
    private val variants = mutableMapOf<String, List<VariantType.Variant>>()
    private val typeByVariantName = mutableMapOf<String, VariantType>()

    fun getTypeOrNull(name: String): Type? = types[name]

    fun getTypeVariants(variantName: String): List<VariantType.Variant>? = variants[variantName]

    fun getTypeByVariantName(variantName: String): VariantType? = typeByVariantName[variantName]

    fun defineTypes(
        moduleName: String,
        packageName: String,
        typeDefs: List<ParseVariantTypeDefinition>,
        resolveTypeRef: (TypeRef, typeParameterNames: Set<String>) -> Type
    ) {
        typeDefs.forEach { addVariantType(moduleName, packageName, it) }
        typeDefs.forEach { addVariantConstructors(it, resolveTypeRef) }
    }

    private fun addVariantType(moduleName: String, packageName: String, typeDefinition: ParseVariantTypeDefinition) {
        types[typeDefinition.typeName] = VariantType(
            moduleName,
            packageName,
            typeDefinition.typeName,
            typeDefinition.typeParameters.map { typeParam -> Type.typeParameter(typeParam.name) },
            emptyMap(),
            null,
        )
    }

    private fun addVariantConstructors(
        typeDefinition: ParseVariantTypeDefinition,
        resolveTypeRef: (TypeRef, typeParameterNames: Set<String>) -> Type
    ) {
        val baseType = (types[typeDefinition.typeName]
            ?: TODO("Type ${typeDefinition.typeName} is not defined here!")) as VariantType
        val variantTypeParameters = baseType.genericTypeParameters.map { it.name }.toSet()
        val variants = typeDefinition.variantConstructors.map {
            VariantType.Variant(
                variantName = it.name,
                fields = it.formalArguments.map { arg ->
                    VariantType.VariantField(arg.name, resolveTypeRef(arg.typeRef, variantTypeParameters))
                }
            )
        }

        this.variants[typeDefinition.typeName] = variants
        variants.forEach { typeByVariantName[it.variantName] = baseType }
        baseType.variant = variants.singleOrNull()
    }
}

