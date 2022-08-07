package gh.marad.chi.core

fun resolveGenericType(
    fnType: FnType,
    callTypeParameters: List<Type>,
): Type {
    assert(callTypeParameters.isEmpty() || fnType.genericTypeParameters.size == callTypeParameters.size) {
        "!!!"
    }
    return if (fnType.returnType.isGenericType()) {
        if (callTypeParameters.isNotEmpty()) {
            (fnType.returnType as GenericType).construct(callTypeParameters)
        } else {
            TODO("Infer the generic type")
        }
    } else {
        fnType.returnType
    }
}
