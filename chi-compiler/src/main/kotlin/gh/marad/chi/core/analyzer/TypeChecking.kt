package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

fun checkThatSymbolNamesAreDefined(expr: Expression, messages: MutableList<Message>) {
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
    if(expr is Fn && expr.body is Block) {
        val expected = expr.returnType
        if (expr.body.body.isEmpty() && expected != Type.unit) {
            messages.add(MissingReturnValue(expected, expr.body.location))
        }
    }
}

fun checkThatFunctionCallsReceiveAppropriateCountOfArguments(expr: Expression, messages: MutableList<Message>) {
    if(expr is FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getSymbol(expr.name)

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

fun checkForOverloadedFunctionCallCandidate(expr: Expression, messages: MutableList<Message>) {
    if (expr is FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getSymbol(expr.name)

        if (valueType is OverloadedFnType) {
            val argumentTypes = expr.parameters.map { it.type }
            scope.getSymbol(expr.name)
            val candidates = valueType.types.count { it.paramTypes == argumentTypes }
            when {
                candidates == 0 -> messages.add(NoCandidatesForFunction(expr.name, argumentTypes, expr.location))
                candidates >= 2 -> TODO("Ambigious definition for ${expr.name}")
            }
        }
    }
}

fun checkThatFunctionCallsActuallyCallFunctions(expr: Expression, messages: MutableList<Message>) {
    if(expr is FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getSymbol(expr.name)

        if (valueType != null && valueType !is FnType && valueType !is OverloadedFnType) {
            messages.add(NotAFunction(expr.name, expr.location))
        }
    }
}

fun checkThatIfElseBranchTypesMatch(expr: Expression, messages: MutableList<Message>) {
    if(expr is IfElse) {
        val thenBlockType = expr.thenBranch.type
        val elseBlockType = expr.elseBranch?.type

        if (elseBlockType != null && thenBlockType != elseBlockType) {
            messages.add(IfElseBranchesTypeMismatch(thenBlockType, elseBlockType, expr.location))
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
            "!" -> if (op.expr.type != Type.bool) {
                messages.add(TypeMismatch(Type.bool, op.expr.type, op.location))
            }
            else -> TODO("Unimplemented prefix operator")
        }
    }

    fun checkAssignment(expr: Assignment) {
        val scope = expr.enclosingScope

        val expectedType = scope.getSymbol(expr.name)

        if (expectedType != null) {
            checkTypeMatches(expectedType, expr.value.type, expr.location)
        }
    }

    fun checkNameDeclaration(expr: NameDeclaration) {
        if(expr.expectedType != null) {
            checkTypeMatches(expr.expectedType, expr.value.type, expr.value.location)
        }
    }

    fun checkFn(expr: Fn) {
        val expected = expr.returnType
        if (expr.returnType == Type.unit) {
            return
        }

        if (expr.body is Block) {
            if(expr.body.body.isNotEmpty()) {
                val actual = expr.body.type
                val location = expr.body.body.last().location
                checkTypeMatches(expected, actual, location)
            }
        } else {
            checkTypeMatches(expected, expr.body.type, expr.body.location)
        }
    }

    fun checkFnCall(expr: FnCall) {
        val scope = expr.enclosingScope
        val valueType = scope.getSymbol(expr.name)

        if (valueType != null && valueType is FnType) {
            valueType.paramTypes.zip(expr.parameters) { definition, passed ->
                val actualType = passed.type
                checkTypeMatches(definition, actualType, passed.location)
            }
        }
    }

    fun checkIfElseType(expr: IfElse) {
        val conditionType = expr.condition.type
        if (conditionType != Type.bool) {
            messages.add(TypeMismatch(Type.bool, conditionType, expr.condition.location))
        }
    }

    fun checkInfixOp(expr: InfixOp) {
        val leftType = expr.left.type
        val rightType = expr.right.type

        if (leftType != rightType) {
            messages.add(TypeMismatch(expected = leftType, rightType, expr.right.location))
        }
    }

    fun checkCast(expr: Cast) {
        // TODO: I'm not sure what to check here
        val exprType = expr.expression.type
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
    it.addVertex("int")
    it.addVertex("float")

    it.addEdge("int", "float")
    it.addEdge("int", "unit")
    it.addEdge("float", "unit")
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

