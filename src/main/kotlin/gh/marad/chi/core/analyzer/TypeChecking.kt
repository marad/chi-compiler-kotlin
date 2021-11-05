package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*

fun checkTypes(scope: Scope, expr: Expression): List<Message> {
    val messages = mutableListOf<Message>()
    when(expr) {
        is Assignment -> checkAssignment(messages, scope, expr)
        is BlockExpression -> checkBlock(messages, scope, expr)
        is Fn -> checkFn(messages, scope, expr)
        is FnCall -> checkFnCall(messages, scope, expr)
        is Atom -> {} // nothing to check
        is VariableAccess -> {} // nothing to check
    }
    return messages
}

private fun typeMatches(expected: Type, actual: Type): Boolean {
    return actual == expected || expected.name == "unit"
}


private fun checkAssignment(messages: MutableList<Message>, scope: Scope, expr: Assignment) {
    if(expr.expectedType != null) {
        val valueType = inferType(scope, expr.value)
        checkTypeMatches(messages, expr.expectedType, valueType, expr.value.location)
    }
    messages.addAll(checkTypes(scope, expr.value))
}

private fun checkBlock(messages: MutableList<Message>, scope: Scope, expr: BlockExpression) {
    messages.addAll(expr.body.flatMap { checkTypes(scope, it) })
}

private fun checkFn(messages: MutableList<Message>, scope: Scope, expr: Fn) {
    val expected = expr.returnType
    val fnScope = Scope.fromExpressions(expr.block.body, scope)
    expr.parameters.forEach { fnScope.defineExternalName(it.name, it.type) }

    if (expr.block.body.isEmpty() && expected != Type.unit) {
        messages.add(MissingReturnValue(expected, expr.block.location))
    } else if(expr.block.body.isNotEmpty()) {
        val actual = inferType(fnScope, expr.block)
        val location = expr.block.body.last().location
        checkTypeMatches(messages, expected, actual, location)
    } else {
        // expected is 'unit' and block is empty - nothing to check here
    }

    messages.addAll(checkTypes(fnScope, expr.block))
}

private fun checkFnCall(messages: MutableList<Message>, scope: Scope, expr: FnCall) {
    val fn = scope.findVariable(expr.name)

    if (fn != null) {
        if (fn is Fn) {
            if (fn.parameters.count() != expr.parameters.count()) {
                messages.add(
                    FunctionArityError(
                        expr.name,
                        fn.parameters.count(),
                        expr.parameters.count(),
                        expr.location
                    )
                )
            }

            fn.parameters.zip(expr.parameters) { definition, passed ->
                val actualType = inferType(scope, passed)
                checkTypeMatches(messages, definition.type, actualType, passed.location)
            }
        } else {
            TODO("Add message that expr is not a function")
        }
    } else {
        if (scope.getExternalNameType(expr.name) == null) {
            TODO("Add message that expr.name is not in scope")
        }
    }
}


private fun checkTypeMatches(messages: MutableList<Message>, expected: Type, actual: Type, location: Location?) {
    if (!typeMatches(expected, actual)) {
        messages.add(TypeMismatch(expected, actual, location))
    }
}


