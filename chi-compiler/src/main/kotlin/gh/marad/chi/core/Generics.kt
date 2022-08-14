package gh.marad.chi.core

fun resolveGenericType(
    fnType: FnType,
    callTypeParameters: List<Type>,
    callParameters: List<Expression>,
): Type {
    assert(callTypeParameters.isEmpty() || fnType.genericTypeParameters.size == callTypeParameters.size)
    val typeByParameterName = typesByTypeParameterName(fnType, callTypeParameters, callParameters)
    return if (fnType.returnType.isGenericType()) {
        (fnType.returnType as GenericType).construct(typeByParameterName)
    } else {
        fnType.returnType
    }
}


fun typesByTypeParameterName(
    fnType: FnType,
    callTypeParameters: List<Type>,
    callParameters: List<Expression>
): Map<GenericTypeParameter, Type> {
    val namesFromTypeParameters = matchTypeParameters(fnType.genericTypeParameters, callTypeParameters)
    val namesFromCallParameters = matchCallTypes(fnType.paramTypes, callParameters.map { it.type })
    // TODO: check that for each name parameters types match
    val result = mutableMapOf<GenericTypeParameter, Type>()
    result.putAll(namesFromTypeParameters)
    result.putAll(namesFromCallParameters)
    return result
}

fun matchTypeParameters(
    definedTypeParameters: List<GenericTypeParameter>,
    callTypeParameters: List<Type>
): Map<GenericTypeParameter, Type> {
    return definedTypeParameters.zip(callTypeParameters).toMap()
}

fun matchCallTypes(definedParameters: List<Type>, callParameters: List<Type>): Map<GenericTypeParameter, Type> {
    val result = mutableMapOf<GenericTypeParameter, Type>()
    definedParameters.zip(callParameters)
        .forEach { (definedParam, callParam) ->
            result.putAll(matchCallTypes(definedParam, callParam))
        }
    return result
}

fun matchCallTypes(definedParam: Type, callParam: Type): Map<GenericTypeParameter, Type> {
    return if (definedParam is GenericTypeParameter) {
        mapOf(definedParam to callParam)
    } else if (definedParam.isTypeConstructor()) {
        assert(callParam.isGenericType()) { "Types does not match!" }
        matchCallTypes(
            definedParameters = (definedParam as GenericType).getTypeParameters(),
            callParameters = (callParam as GenericType).getTypeParameters()
        )
    } else emptyMap()
}
