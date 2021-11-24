package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

fun checkTypes(expr: Expression): List<Message> {
    val messages = mutableListOf<Message>()
    // this val here is so that `when` give error instead of warn on non-exhaustive match
    val ignored: Any = when(expr) {
        is Program -> checkExprs(messages, expr.expressions)
        is Assignment -> checkAssignment(messages, expr)
        is NameDeclaration -> checkNameDeclaration(messages, expr)
        is Block -> checkExprs(messages, expr.body)
        is Fn -> checkFn(messages, expr)
        is FnCall -> checkFnCall(messages, expr)
        is Atom -> {} // nothing to check
        is VariableAccess -> {} // nothing to check
        is IfElse -> checkIfElseType(messages, expr)
        is InfixOp -> checkInfixOp(messages, expr)
        is PrefixOp -> checkPrefixOp(messages, expr)
        is Cast -> checkCast(messages, expr)
    }
    return messages
}

fun checkPrefixOp(messages: MutableList<Message>, op: PrefixOp) {
    when(op.op) {
        "!" -> if (inferType(op.expr) != Type.bool) {
            messages.add(TypeMismatch(Type.bool, inferType(op.expr), op.location))
        }
        else -> TODO("Unimplemented prefix operator")
    }
}

private fun checkAssignment(messages: MutableList<Message>, expr: Assignment) {
    val scope = expr.enclosingScope

    val expectedType = scope.getLocalName(expr.name)?.let { inferType(it) }
        ?: scope.getExternalNameType(expr.name)

    if (expectedType != null) {
        val actualType = inferType(expr.value)
        checkTypeMatches(messages, expectedType, actualType, expr.location)
    } else {
        messages.add(UnrecognizedName(expr.name, expr.location))
    }
}

private fun checkNameDeclaration(messages: MutableList<Message>, expr: NameDeclaration) {
    if(expr.expectedType != null) {
        val valueType = inferType(expr.value)
        checkTypeMatches(messages, expr.expectedType, valueType, expr.value.location)
    }
    messages.addAll(checkTypes(expr.value))
}

private fun checkExprs(messages: MutableList<Message>, exprs: List<Expression>) {
    messages.addAll(exprs.flatMap { checkTypes(it) })
}

private fun checkFn(messages: MutableList<Message>, expr: Fn) {
    val expected = expr.returnType
    if (expr.block.body.isEmpty() && expected != Type.unit) {
        messages.add(MissingReturnValue(expected, expr.block.location))
    } else if(expr.block.body.isNotEmpty()) {
        val actual = inferType(expr.block)
        val location = expr.block.body.last().location
        checkTypeMatches(messages, expected, actual, location)
    } else {
        // expected is 'unit' and block is empty - nothing to check here
    }

    messages.addAll(checkTypes(expr.block))
}

private fun checkFnCall(messages: MutableList<Message>, expr: FnCall) {
    val scope = expr.enclosingScope
    val valueType = scope.getLocalName(expr.name)?.let { inferType(it) }
        ?: scope.getExternalNameType(expr.name)

    if (valueType != null) {
        if (valueType is FnType) {
            if (valueType.paramTypes.count() != expr.parameters.count()) {
                messages.add(
                    FunctionArityError(
                        expr.name,
                        valueType.paramTypes.count(),
                        expr.parameters.count(),
                        expr.location
                    )
                )
            }

            valueType.paramTypes.zip(expr.parameters) { definition, passed ->
                val actualType = inferType(passed)
                checkTypeMatches(messages, definition, actualType, passed.location)
            }
        } else {
            messages.add(NotAFunction(expr.name, expr.location))
        }
    } else {
        messages.add(UnrecognizedName(expr.name, expr.location))
    }
}

fun checkIfElseType(messages: MutableList<Message>, expr: IfElse) {
    val conditionType = inferType(expr.condition)
    val thenBlockType = inferType(expr.thenBranch)
    val elseBlockType = expr.elseBranch?.let { inferType(it) }

    if (conditionType != Type.bool) {
        messages.add(TypeMismatch(Type.bool, conditionType, expr.condition.location))
    }

    if (elseBlockType != null && thenBlockType != elseBlockType) {
        messages.add(IfElseBranchesTypeMismatch(thenBlockType, elseBlockType))
    }
}


fun checkInfixOp(messages: MutableList<Message>, expr: InfixOp) {
    val leftType = inferType(expr.left)
    val rightType = inferType(expr.right)

    if (leftType != rightType) {
        messages.add(TypeMismatch(expected = leftType, rightType, expr.right.location))
    }
}

fun checkCast(messages: MutableList<Message>, expr: Cast) {
    // TODO: I'm not sure what to check here
    val exprType = inferType(expr.expression)
    if (exprType != expr.targetType) {
        if (expr.targetType == Type.bool) {
            checkTypeMatches(messages, expr.targetType, exprType, expr.location)
        }
    }

}

private var typeGraph: Graph<String, DefaultEdge> = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java).also {
    it.addVertex("unit")
    it.addVertex("i32")
    it.addVertex("i64")
    it.addVertex("f32")
    it.addVertex("f64")

    it.addEdge("i32", "i64")
    it.addEdge("f32", "f64")
    it.addEdge("i32", "f32")
    it.addEdge("i64", "f64")
}

private fun isSubType(subtype: Type, supertype: Type): Boolean {
    return if (typeGraph.containsVertex(subtype.name) && typeGraph.containsVertex(supertype.name)) {
        val dijkstraAlgo = DijkstraShortestPath(typeGraph)
        val path = dijkstraAlgo.getPath(subtype.name, supertype.name)
        path != null
    } else {
        false
    }
}


private fun checkTypeMatches(messages: MutableList<Message>, expected: Type, actual: Type, location: Location?) {
    val typeMatches = expected == actual || isSubType(actual, expected)
    if (!typeMatches) {
        messages.add(TypeMismatch(expected, actual, location))
    }
}