package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

class Scope(private val parentScope: Scope? = null) {
    private val variables = mutableMapOf<String, Expression>()
    private val functionParams = mutableMapOf<String, Type>()

    fun defineVariable(name: String, value: Expression) {
        variables[name] = value
    }

    fun defineFunctionParams(params: List<FnParam>) {
        params.forEach { functionParams[it.name] = it.type }
    }

    fun getFunctionParamType(paramName: String): Type? = functionParams[paramName]

    fun findVariable(name: String, location: Location?): Expression =
        // TODO Better exception
        variables[name]
            ?: parentScope?.findVariable(name, location)
            ?: throw RuntimeException("There is no variable '${name}' in scope. Error at ${location?.formattedPosition}")

    // TODO Better exceptions
    fun findFunction(name: String, location: Location?): Fn {
        val function = findVariable(name, location)
        if (function is Fn) {
            return function
        } else {
            throw RuntimeException("Variable '$name' is not a function")
        }
    }

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

