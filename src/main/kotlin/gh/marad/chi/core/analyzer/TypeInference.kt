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
        is FnCall ->
            (scope.findVariable(expr.name) as Fn?)?.returnType
                ?: (scope.getExternalNameType(expr.name) as FnType?)?.returnType
                ?: throw MissingVariable(expr.name, expr.location)
        is VariableAccess ->
            scope.findVariable(expr.name)?.let { inferType(scope, it) }
                ?: scope.getExternalNameType(expr.name)
                ?: throw MissingVariable(expr.name, expr.location)
    }
}

class MissingVariable(val name: String, val location: Location?) :
        RuntimeException("Variable '$name' not found in scope at ${location?.formattedPosition}")