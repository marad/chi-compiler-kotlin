package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

class Scope(private val parentScope: Scope? = null) {
    private val variables = mutableMapOf<String, Expression>()
    private val externalNames = mutableMapOf<String, Type>()

    fun defineVariable(name: String, value: Expression) {
        variables[name] = value
    }

    fun defineExternalName(name: String, type: Type) {
        externalNames[name] = type
    }

    fun getExternalNameType(externalName: String): Type? =
        externalNames[externalName]
            ?: parentScope?.getExternalNameType(externalName)

    fun findVariable(name: String): Expression? =
        variables[name]
            ?: parentScope?.findVariable(name)

    companion object {
        fun fromExpressions(expression: List<Expression>, parentScope: Scope? = null): Scope {
            val scope = Scope(parentScope)
            expression.forEach { expr ->
                when(expr) {
                    is Assignment -> scope.defineVariable(expr.name, expr.value)
                    is Atom -> {}
                    is BlockExpression -> {}
                    is Fn -> {}
                    is FnCall -> {}
                    is VariableAccess -> {}
                }
            }
            return scope
        }
    }
}

