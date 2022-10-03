package gh.marad.chi.core.namespace

import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.parser.readers.*

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
        getTypeByName: (String) -> Type,
        getVariants: (VariantType) -> List<VariantType.Variant>
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
                    paramTypes = ref.argumentTypeRefs.map {
                        resolve(
                            it,
                            typeParameterNames,
                            getTypeByName,
                            getVariants
                        )
                    },
                    returnType = resolve(ref.returnType, typeParameterNames, getTypeByName, getVariants)
                )
            is TypeConstructorRef -> {
                // TODO: sprawdź, że typ isTypeConstructor()
                // TODO: sprawdź, że ma tyle samo type parametrów co podane
                val allTypeParameterNames =
                    typeParameterNames + ref.typeParameters.filterIsInstance<TypeParameter>()
                        .map { it.name }.toSet()
                val type = resolve(ref.baseType, allTypeParameterNames, getTypeByName, getVariants)
                val parameterTypes =
                    ref.typeParameters.map { resolve(it, allTypeParameterNames, getTypeByName, getVariants) }
                type.applyTypeParameters(parameterTypes)
            }
            is VariantNameRef -> {
                val variantType =
                    resolve(ref.variantType, typeParameterNames, getTypeByName, getVariants) as VariantType
                val variant = getVariants(variantType).find { it.variantName == ref.variantName }
                variantType.withVariant(variant)
            }
            is TypeParameter -> Type.typeParameter(ref.name)
        }
    }

}

