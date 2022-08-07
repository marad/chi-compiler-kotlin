package gh.marad.chi.core

//fun resolveGenericTypes(expr: Expression): Expression {
//    return mapAst(expr) {
//        if (it.type.isGenericType()) {
//            val genericType = it.type as GenericType
//            expr.updateType(genericType.construct())
//        } else {
//            expr
//        }
//    }
//}

/**
fnReturnType - function re
 */
fun resolveGenericType(
    fnType: FnType,
    callTypeParameters: List<Type>,
    callArgumentTypes: List<Type>
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
