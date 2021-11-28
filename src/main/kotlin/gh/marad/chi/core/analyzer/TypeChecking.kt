package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

fun checkThatSymbolNamesAreDefined(expr: Expression, messages: MutableList<Message>) {
    fun CompilationScope.containsSymbol(name: String) =
        getLocalName(name) != null || getExternalNameType(name) != null || getParameter(name) != null

    when(expr) {
        is FnCall -> {
            if (!expr.enclosingScope.containsSymbol(expr.name)) {
                messages.add(UnrecognizedName(expr.name, expr.location))
            }
        }
        is VariableAccess -> {
            if (!expr.enclosingScope.containsSymbol(expr.name)) {
                messages.add(UnrecognizedName(expr.name, expr.location))
            }
        }
        else -> {}
    }
}

fun checkThatFunctionHasAReturnValue(expr: Expression, messages: MutableList<Message>) {
    if(expr is Fn) {
        val expected = expr.returnType
        if (expr.block.body.isEmpty() && expected != Type.unit) {
            messages.add(MissingReturnValue(expected, expr.block.location))
        }
    }
}

fun checkThatFunctionCallsReceiveAppropriateCountOfArguments(expr: Expression, messages: MutableList<Message>) {
    if(expr is FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getLocalName(expr.name)?.let { inferType(it) }
            ?: scope.getExternalNameType(expr.name)

        if (valueType != null && valueType is FnType &&
            valueType.paramTypes.count() != expr.parameters.count()) {
            messages.add(
                FunctionArityError(
                    expr.name,
                    valueType.paramTypes.count(),
                    expr.parameters.count(),
                    expr.location
                )
            )
        }
    }
}

fun checkThatFunctionCallsActuallyCallFunctions(expr: Expression, messages: MutableList<Message>) {
    if(expr is FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getLocalName(expr.name)?.let { inferType(it) }
            ?: scope.getExternalNameType(expr.name)

        if (valueType != null && valueType !is FnType) {
            messages.add(NotAFunction(expr.name, expr.location))
        }
    }
}

fun checkThatIfElseBranchTypesMatch(expr: Expression, messages: MutableList<Message>) {
    if(expr is IfElse) {
        val thenBlockType = inferType(expr.thenBranch)
        val elseBlockType = expr.elseBranch?.let { inferType(it) }

        if (elseBlockType != null && thenBlockType != elseBlockType) {
            messages.add(IfElseBranchesTypeMismatch(thenBlockType, elseBlockType))
        }
    }
}

fun checkTypes(expr: Expression, messages: MutableList<Message>) {

    fun checkTypeMatches(expected: Type, actual: Type, location: Location?) {
        val typeMatches = expected == actual || isSubType(actual, expected)
        if (!typeMatches) {
            messages.add(TypeMismatch(expected, actual, location))
        }
    }


    fun checkPrefixOp(op: PrefixOp) {
        when(op.op) {
            "!" -> if (inferType(op.expr) != Type.bool) {
                messages.add(TypeMismatch(Type.bool, inferType(op.expr), op.location))
            }
            else -> TODO("Unimplemented prefix operator")
        }
    }

    fun checkAssignment(expr: Assignment) {
        val scope = expr.enclosingScope

        val expectedType = scope.getLocalName(expr.name)?.let { inferType(it) }
            ?: scope.getExternalNameType(expr.name)

        if (expectedType != null) {
            val actualType = inferType(expr.value)
            checkTypeMatches(expectedType, actualType, expr.location)
        }
    }

    fun checkNameDeclaration(expr: NameDeclaration) {
        if(expr.expectedType != null) {
            val valueType = inferType(expr.value)
            checkTypeMatches(expr.expectedType, valueType, expr.value.location)
        }
    }

    fun checkFn(expr: Fn) {
        val expected = expr.returnType
        if(expr.block.body.isNotEmpty() && expected != Type.unit) {
            val actual = inferType(expr.block)
            val location = expr.block.body.last().location
            checkTypeMatches(expected, actual, location)
        }
    }

    fun checkFnCall(expr: FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getLocalName(expr.name)?.let { inferType(it) }
            ?: scope.getExternalNameType(expr.name)

        if (valueType != null && valueType is FnType) {
            valueType.paramTypes.zip(expr.parameters) { definition, passed ->
                val actualType = inferType(passed)
                checkTypeMatches(definition, actualType, passed.location)
            }
        }
    }

    fun checkIfElseType(expr: IfElse) {
        val conditionType = inferType(expr.condition)
        if (conditionType != Type.bool) {
            messages.add(TypeMismatch(Type.bool, conditionType, expr.condition.location))
        }
    }

    fun checkInfixOp(expr: InfixOp) {
        val leftType = inferType(expr.left)
        val rightType = inferType(expr.right)

        if (leftType != rightType) {
            messages.add(TypeMismatch(expected = leftType, rightType, expr.right.location))
        }
    }

    fun checkCast(expr: Cast) {
        // TODO: I'm not sure what to check here
        val exprType = inferType(expr.expression)
        if (exprType != expr.targetType) {
            if (expr.targetType == Type.bool) {
                checkTypeMatches(expr.targetType, exprType, expr.location)
            }
        }

    }

    val ignored: Any = when(expr) {
        is Program -> {} // nothing to check
        is Assignment -> checkAssignment(expr)
        is NameDeclaration -> checkNameDeclaration(expr)
        is Block -> {} // nothing to check
        is Fn -> checkFn(expr)
        is FnCall -> checkFnCall(expr)
        is Atom -> {} // nothing to check
        is VariableAccess -> {} // nothing to check
        is IfElse -> checkIfElseType(expr)
        is InfixOp -> checkInfixOp(expr)
        is PrefixOp -> checkPrefixOp(expr)
        is Cast -> checkCast(expr)
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

fun isSubType(subtype: Type, supertype: Type): Boolean {
    return if (subtype != supertype && typeGraph.containsVertex(subtype.name) && typeGraph.containsVertex(supertype.name)) {
        val dijkstraAlgo = DijkstraShortestPath(typeGraph)
        val path = dijkstraAlgo.getPath(subtype.name, supertype.name)
        path != null
    } else {
        false
    }
}

