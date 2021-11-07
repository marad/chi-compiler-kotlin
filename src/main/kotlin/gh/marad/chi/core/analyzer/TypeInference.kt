package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

fun inferType(expr: Expression): Type {
    return when(expr) {
        is Assignment -> inferType(expr.value)
        is NameDeclaration -> expr.expectedType ?: inferType(expr.value)
        is Atom -> expr.type
        is Block -> expr.body.lastOrNull()?.let { inferType(it) }
            ?: Type.unit
        is Fn -> FnType(
            paramTypes = expr.parameters.map { it.type },
            returnType = expr.returnType,
        )
        is FnCall -> inferFnCallType(expr)
        is VariableAccess ->
            expr.enclosingScope.getLocalName(expr.name)
                ?.let(::inferType)
                ?: expr.enclosingScope.getParameter(expr.name)
                ?: expr.enclosingScope.getExternalNameType(expr.name)
                ?: throw MissingVariable(expr.name, expr.location)
        is IfElse -> inferType(expr.thenBranch)
    }
}

class MissingVariable(val name: String, val location: Location?) :
        RuntimeException("Variable '$name' not found in scope at ${location?.formattedPosition}")

class FunctionExpected(val name: String, val location: Location?) :
        RuntimeException("Variable '$name' is not a function at ${location?.formattedPosition}")

private fun inferFnCallType(fnCall: FnCall): Type {
    val variableType = fnCall.enclosingScope.getLocalName(fnCall.name)
        ?.let(::inferType)
        ?: fnCall.enclosingScope.getParameter(fnCall.name)
        ?: fnCall.enclosingScope.getExternalNameType(fnCall.name)
        ?: throw MissingVariable(fnCall.name, fnCall.location)

    return if (variableType is FnType) {
        variableType.returnType
    } else {
        throw FunctionExpected(fnCall.name, fnCall.location)
    }
}
