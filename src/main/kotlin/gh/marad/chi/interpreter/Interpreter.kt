package gh.marad.chi.interpreter

import gh.marad.chi.core.*

class Scope(names: Map<String, Expression> = mapOf()) {
    val names: MutableMap<String, Expression> = names.toMutableMap()

    fun copy(): Scope =
        Scope(names)

    fun eval(expression: Expression): Expression {
        return when (expression) {
            is Atom -> expression
            is VariableAccess -> evalVariableAccess(expression)
            is Assignment -> evalAssignment(expression)
            is BlockExpression -> evalBlockExpression(expression)
            is Fn -> expression
            is FnCall -> evalFnCall(expression)
        }
    }

    private fun evalVariableAccess(expr: VariableAccess): Expression {
        return names[expr.name]
            ?: throw RuntimeException("Name '${expr.name}' is not recognized")
    }

    private fun evalAssignment(expr: Assignment): Expression {
        // TODO: brak wykorzystania mutable/immutable
        // pewnie names powinno mieć jakiś dodatkowy typ Variable, który by o tym decydował
        // być może to jest spoko bo to się powinno wywalić na etapie analizy
        val result = eval(expr.value)
        names[expr.name] = result
        return result
    }

    private fun evalBlockExpression(expr: BlockExpression): Expression {
        return expr.body.map { eval(it) }.last()
    }

    private fun evalFnCall(expr: FnCall): Expression {
        val fn = names[expr.name] as Fn
        val subscope = copy()
        fn.parameters
            .zip(expr.parameters.map { eval(it) })
            .forEach {
                subscope.names[it.first.name] = it.second
            }

        return subscope.eval(fn.body)
    }
}