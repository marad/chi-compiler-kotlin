package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

fun inferType(scope: Scope, expr: Expression): Type {
    return when(expr) {
        is Assignment -> expr.expectedType ?: inferType(scope, expr.value)
        is Atom -> expr.type
        is BlockExpression -> expr.body.lastOrNull()?.let { inferType(scope, it) }
            ?: Type.unit
        is Fn -> Type.fn
        is FnCall -> scope.findFunction(expr.name, expr.location).returnType
        is VariableAccess -> scope.getFunctionParamType(expr.name) ?: inferType(scope, scope.findVariable(expr.name, expr.location))
    }
}

