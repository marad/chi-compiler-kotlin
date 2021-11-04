package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

class Scope(private val parentScope: Scope? = null) {
    private val functions = mutableMapOf<String, Fn>()
    private val variables = mutableMapOf<String, Expression>()
    private val functionParams = mutableMapOf<String, Type>()

    fun defineVariable(name: String, value: Expression) {
        variables[name] = value
    }

    fun defineFunction(name: String, value: Fn) {
        functions[name] = value
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

    fun findFunction(name: String, location: Location?): Fn =
        // TODO Better exception
        functions[name]
            ?: parentScope?.findFunction(name, location)
            ?: throw RuntimeException("There is no function '$name' in scope. Error at ${location?.formattedPosition}")


    companion object {
        fun fromExpressions(expression: List<Expression>, parentScope: Scope? = null): Scope {
            val scope = Scope(parentScope)
            expression.forEach { expr ->
                when(expr) {
                    is Assignment -> {
                        when (expr.value) {
                            is Fn -> scope.defineFunction(expr.name, expr.value)
                            else -> scope.defineVariable(expr.name, expr.value)
                        }
                    }
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

