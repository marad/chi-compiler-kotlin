package gh.marad.chi.core

@Suppress("UNUSED_VARIABLE")
fun forEachAst(expression: Expression, func: (Expression) -> Unit) {
    val ignored = when (expression) {
        is Assignment -> {
            forEachAst(expression.value, func)
            func(expression)
        }
        is Atom -> func(expression)
        is InterpolatedString -> {
            expression.parts.forEach { forEachAst(it, func) }
            func(expression)
        }
        is Block -> {
            expression.body.forEach { forEachAst(it, func) }
            func(expression)
        }
        is Cast -> {
            forEachAst(expression.expression, func)
            func(expression)
        }
        is Fn -> {
            forEachAst(expression.body, func)
            func(expression)
        }
        is FnCall -> {
            forEachAst(expression.function, func)
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
        is Package -> {
            func(expression)
        }
        is Import -> {
            func(expression)
        }
        is VariableAccess -> {
            func(expression)
        }
        is FieldAccess -> {
            forEachAst(expression.receiver, func)
            func(expression)
        }
        is FieldAssignment -> {
            forEachAst(expression.receiver, func)
            forEachAst(expression.value, func)
            func(expression)
        }
        is Group -> {
            forEachAst(expression.value, func)
            func(expression)
        }
        is WhileLoop -> {
            forEachAst(expression.condition, func)
            forEachAst(expression.loop, func)
            func(expression)
        }
        is IndexOperator -> {
            forEachAst(expression.variable, func)
            forEachAst(expression.index, func)
            func(expression)
        }
        is IndexedAssignment -> {
            forEachAst(expression.variable, func)
            forEachAst(expression.index, func)
            forEachAst(expression.value, func)
            func(expression)
        }
        is DefineVariantType -> {
            func(expression)
        }
        is Is -> {
            forEachAst(expression.value, func)
            func(expression)
        }
        is Break -> {
            func(expression)
        }
        is Continue -> {
            func(expression)
        }
        is EffectDefinition -> {
            func(expression)
        }
        is Handle -> {
            forEachAst(expression.body, func)
            expression.cases.forEach {
                forEachAst(it.body, func)
            }
            func(expression)
        }
    }
}

fun mapAst(expression: Expression, func: (Expression) -> Expression): Expression {
    return when (expression) {
        is Assignment -> {
            val mappedValue = mapAst(expression.value, func)
            expression.copy(value = mappedValue)
        }
        is Atom -> func(expression)
        is InterpolatedString -> {
            val mappedParts = expression.parts.map { mapAst(it, func) }
            func(expression.copy(parts = mappedParts))
        }
        is Block -> {
            val mappedBody = expression.body.map { mapAst(it, func) }
            func(expression.copy(body = mappedBody))
        }
        is Cast -> {
            val mappedInnerExpression = mapAst(expression.expression, func)
            func(expression.copy(expression = mappedInnerExpression))
        }
        is Fn -> {
            val mappedBlock = mapAst(expression.body, func) as Block
            func(expression.copy(body = mappedBlock))
        }
        is FnCall -> {
            val mappedFunction = mapAst(expression.function, func)
            val mappedParams = expression.parameters.map { mapAst(it, func) }
            func(
                expression.copy(
                    function = mappedFunction,
                    parameters = mappedParams
                )
            )
        }
        is IfElse -> {
            func(
                expression.copy(
                    condition = mapAst(expression.condition, func),
                    thenBranch = mapAst(expression.thenBranch, func),
                    elseBranch = expression.elseBranch?.let { mapAst(it, func) },
                )
            )
        }
        is InfixOp -> {
            func(
                expression.copy(
                    right = mapAst(expression.right, func),
                    left = mapAst(expression.left, func),
                )
            )
        }
        is NameDeclaration -> {
            func(
                expression.copy(
                    value = mapAst(expression.value, func)
                )
            )
        }
        is PrefixOp -> {
            func(
                expression.copy(
                    expr = mapAst(expression.expr, func)
                )
            )
        }
        is Program -> {
            func(expression.copy(
                expressions = expression.expressions.map { mapAst(it, func) }
            ))
        }
        is Package -> {
            func(expression)
        }
        is Import -> {
            func(expression)
        }
        is VariableAccess -> {
            func(expression)
        }
        is FieldAccess -> {
            func(
                expression.copy(
                    receiver = mapAst(expression.receiver, func)
                )
            )
        }
        is FieldAssignment -> {
            func(
                expression.copy(
                    receiver = mapAst(expression.receiver, func),
                    value = mapAst(expression.value, func)
                )
            )
        }
        is Group -> {
            func(expression.copy(value = mapAst(expression.value, func)))
        }
        is WhileLoop -> {
            func(expression.copy(condition = mapAst(expression.condition, func), loop = mapAst(expression.loop, func)))
        }
        is IndexOperator -> {
            func(
                expression.copy(
                    variable = mapAst(expression.variable, func),
                    index = mapAst(expression.index, func)
                )
            )
        }
        is IndexedAssignment -> {
            func(
                expression.copy(
                    variable = mapAst(expression.variable, func),
                    index = mapAst(expression.index, func),
                    value = mapAst(expression.value, func)
                )
            )
        }
        is DefineVariantType -> {
            func(expression)
        }
        is Is -> {
            func(
                expression.copy(
                    value = mapAst(expression.value, func)
                )
            )
        }
        is Break -> {
            func(expression)
        }
        is Continue -> {
            func(expression)
        }
        is EffectDefinition -> func(expression)
        is Handle -> {
            func(
                expression.copy(
                    body = mapAst(expression.body, func) as Block,
                    cases = expression.cases.map {
                        it.copy(body = mapAst(it.body, func))
                    }
                )
            )
        }
    }

}
