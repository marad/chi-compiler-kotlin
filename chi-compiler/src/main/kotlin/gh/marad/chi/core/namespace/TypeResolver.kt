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

