package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

fun inferType(scope: Scope, expr: Expression): Type {
    return when(expr) {
        is Assignment -> expr.expectedType ?: inferType(scope, expr.value)
        is Atom -> expr.type
        is BlockExpression -> expr.body.lastOrNull()?.let { inferType(scope, it) }
            ?: Type.unit
        is Fn -> FnType(
            paramTypes = expr.parameters.map { it.type },
            returnType = expr.returnType,
        )
        is FnCall ->
            (scope.findVariable(expr.name) as Fn?)?.returnType
                ?: scope.getExternalNameType(expr.name)
                ?: throw RuntimeException("Unrecognized function '${expr.name}'")
        is VariableAccess ->
            scope.findVariable(expr.name)?.let { inferType(scope, it) }
                ?: scope.getExternalNameType(expr.name)
                ?: throw RuntimeException("Unrecognized variable '${expr.name}'")
    }
}
