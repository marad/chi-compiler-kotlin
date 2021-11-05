package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

fun inferType(scope: Scope, expr: Expression): Type {
    return when(expr) {
        is Assignment -> inferType(scope, expr.value)
        is NameDeclaration -> expr.expectedType ?: inferType(scope, expr.value)
        is Atom -> expr.type
        is BlockExpression -> expr.body.lastOrNull()?.let { inferType(scope, it) }
            ?: Type.unit
        is Fn -> FnType(
            paramTypes = expr.parameters.map { it.type },
            returnType = expr.returnType,
        )
        is FnCall -> inferFnCallType(scope, expr)
        is VariableAccess -> getVariableType(scope, expr.name, expr.location)
        is IfElse -> inferType(scope, expr.thenBranch)
    }
}

class MissingVariable(val name: String, val location: Location?) :
        RuntimeException("Variable '$name' not found in scope at ${location?.formattedPosition}")

class FunctionExpected(val name: String, val location: Location?) :
        RuntimeException("Variable '$name' is not a function at ${location?.formattedPosition}")

private fun inferFnCallType(scope: Scope, fnCall: FnCall): Type {
    val variableType = getVariableType(scope, fnCall.name, fnCall.location)

    return if (variableType is FnType) {
        variableType.returnType
    } else {
        throw FunctionExpected(fnCall.name, fnCall.location)
    }
}


private fun getVariableType(scope: Scope, name: String, location: Location?): Type {
    return scope.findVariable(name)?.let { inferType(scope, it) }
        ?: scope.getExternalNameType(name)
        ?: throw MissingVariable(name, location)
}
