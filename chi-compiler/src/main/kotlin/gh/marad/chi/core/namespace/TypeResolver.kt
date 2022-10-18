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
            is TypeNameRef ->
                resolveNameTypeRef(typeParameterNames, ref, getTypeByName)

            is FunctionTypeRef ->
                resolveFunctionTypeRef(ref, typeParameterNames, getTypeByName, getVariants)

            is TypeConstructorRef ->
                resolveTypeConstructorRef(typeParameterNames, ref, getTypeByName, getVariants)

            is VariantNameRef ->
                resolveVariantNameRef(ref, typeParameterNames, getTypeByName, getVariants)

            is TypeParameterRef ->
                Type.typeParameter(ref.name)
        }
    }

    private fun resolveNameTypeRef(
        typeParameterNames: Set<String>,
        ref: TypeNameRef,
        getTypeByName: (String) -> Type
    ) = if (typeParameterNames.contains(ref.typeName)) {
        Type.typeParameter(ref.typeName)
    } else {
        getTypeByName(ref.typeName)
    }

    private fun resolveFunctionTypeRef(
        ref: FunctionTypeRef,
        typeParameterNames: Set<String>,
        getTypeByName: (String) -> Type,
        getVariants: (VariantType) -> List<VariantType.Variant>
    ): FnType = FnType(
        genericTypeParameters = ref.typeParameters.filterIsInstance<TypeParameterRef>()
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

    private fun resolveTypeConstructorRef(
        typeParameterNames: Set<String>,
        ref: TypeConstructorRef,
        getTypeByName: (String) -> Type,
        getVariants: (VariantType) -> List<VariantType.Variant>
    ): Type {
        // TODO: sprawdź, że typ isTypeConstructor()
        // TODO: sprawdź, że ma tyle samo type parametrów co podane
        val allTypeParameterNames =
            typeParameterNames + ref.typeParameters.filterIsInstance<TypeParameterRef>()
                .map { it.name }.toSet()
        val type = resolve(ref.baseType, allTypeParameterNames, getTypeByName, getVariants)
        val parameterTypes =
            ref.typeParameters.map { resolve(it, allTypeParameterNames, getTypeByName, getVariants) }
        return type.applyTypeParameters(parameterTypes)
    }

    private fun resolveVariantNameRef(
        ref: VariantNameRef,
        typeParameterNames: Set<String>,
        getTypeByName: (String) -> Type,
        getVariants: (VariantType) -> List<VariantType.Variant>
    ): VariantType {
        val variantType =
            resolve(ref.variantType, typeParameterNames, getTypeByName, getVariants) as VariantType
        val variant = getVariants(variantType).find { it.variantName == ref.variantName }
        return variantType.withVariant(variant)
    }

}

