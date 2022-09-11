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

    fun getType(name: String): Type {
        // TODO: sprawdź, że nazwa pod tym typem istnieje!!
        return types[name] ?: TODO("Type $name not found!")
    }

    fun getTypeVariants(variantName: String): List<VariantType.Variant>? = variants[variantName]

    fun addVariantType(moduleName: String, packageName: String, typeDefinition: ParseVariantTypeDefinition) {
        types[typeDefinition.typeName] = VariantType(
            moduleName,
            packageName,
            typeDefinition.typeName,
            typeDefinition.typeParameters.map { typeParam -> Type.typeParameter(typeParam.name) },
            emptyMap(),
            null,
        )
    }

    fun addVariantConstructors(
        typeDefinition: ParseVariantTypeDefinition,
        resolveTypeRef: (TypeRef, typeParameterNames: Set<String>) -> Type
    ) {
        val baseType = getType(typeDefinition.typeName) as VariantType
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
        baseType.variant = variants.singleOrNull()
    }
}

