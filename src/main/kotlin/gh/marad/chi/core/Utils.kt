package gh.marad.chi.core

fun forEachAst(expression: Expression, func: (Expression) -> Unit) {
    when(expression) {
        is Assignment -> {
            forEachAst(expression.value, func)
            func(expression)
        }
        is Atom -> func(expression)
        is Block -> {
            expression.body.forEach { forEachAst(it, func) }
            func(expression)
        }
        is Cast -> {
            forEachAst(expression.expression, func)
            func(expression)
        }
        is Fn -> {
            forEachAst(expression.block, func)
            func(expression)
        }
        is FnCall -> {
            expression.parameters.map { forEachAst(it, func) }
            func(expression)
        }
        is IfElse -> {
            forEachAst(expression.condition, func)
            forEachAst(expression.thenBranch, func)
            expression.elseBranch?.let { forEachAst(it, func) }
            func(expression)
        }
        is InfixOp -> {
            forEachAst(expression.right, func)
            forEachAst(expression.left, func)
            func(expression)
        }
        is NameDeclaration -> {
            forEachAst(expression.value, func)
            func(expression)
        }
        is PrefixOp -> {
            forEachAst(expression.expr, func)
            func(expression)
        }
        is Program -> {
            expression.expressions.map { forEachAst(it, func) }
            func(expression)
        }
        is VariableAccess -> {
            func(expression)
        }
    }
}

fun mapAst(expression: Expression, func: (Expression) -> Expression): Expression {
    return when(expression) {
        is Assignment -> {
            val mappedValue = mapAst(expression.value, func)
            expression.copy(value = mappedValue)
        }
        is Atom -> func(expression)
        is Block -> {
            val mappedBody = expression.body.map { mapAst(it, func) }
            func(expression.copy(body = mappedBody))
        }
        is Cast -> {
            val mappedInnerExpression = mapAst(expression.expression, func)
            func(expression.copy(expression = mappedInnerExpression))
        }
        is Fn -> {
            val mappedBlock = mapAst(expression.block, func) as Block
            func(expression.copy(block = mappedBlock))
        }
        is FnCall -> {
            val mappedParams = expression.parameters.map { mapAst(it, func) }
            func(expression.copy(parameters = mappedParams))
        }
        is IfElse -> {
            func(expression.copy(
                condition = expression.let { mapAst(it.condition, func) },
                thenBranch = expression.thenBranch.let { mapAst(it, func) } as Block,
                elseBranch = expression.elseBranch?.let { mapAst(it, func) } as Block?,
            ))
        }
        is InfixOp -> {
            func(expression.copy(
                right = mapAst(expression.right, func),
                left = mapAst(expression.left, func),
            ))
        }
        is NameDeclaration -> {
            func(expression.copy(
                value = mapAst(expression.value, func)
            ))
        }
        is PrefixOp -> {
            func(expression.copy(
                expr = mapAst(expression.expr, func)
            ))
        }
        is Program -> {
            func(expression.copy(
                expressions = expression.expressions.map { mapAst(it, func) }
            ))
        }
        is VariableAccess -> {
            func(expression)
        }
    }

}