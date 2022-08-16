package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

fun checkModuleAndPackageNames(expr: Expression, messages: MutableList<Message>) {
    if (expr is Package) {
        if (expr.moduleName.isEmpty()) {
            messages.add(InvalidModuleName(expr.moduleName, expr.location))
        }
        if (expr.packageName.isEmpty()) {
            messages.add(InvalidPackageName(expr.packageName, expr.location))
        }
    }
}

fun checkImports(expr: Expression, messages: MutableList<Message>) {
    if (expr is Import) {
        if (expr.moduleName.isEmpty()) {
            messages.add(InvalidModuleName(expr.moduleName, expr.location))
        }
        if (expr.packageName.isEmpty()) {
            messages.add(InvalidPackageName(expr.packageName, expr.location))
        }
    }
}

fun checkThatTypesContainAccessedMembers(expr: Expression, messages: MutableList<Message>) {
    if (expr is FieldAccess && expr.receiver.type.isCompositeType()) {
        val hasMember = (expr.receiver.type as CompositeType).hasMember(expr.fieldName)
        if (!hasMember) {
            messages.add(MemberDoesNotExist(expr.receiver.type, expr.fieldName, expr.location))
        }
    }
}

fun checkThatVariableIsDefined(expr: Expression, messages: MutableList<Message>) {
    if (expr is VariableAccess) {
        if (!expr.definitionScope.containsSymbol(expr.name)) {
            messages.add(UnrecognizedName(expr.name, expr.location))
        }
    }
}

fun checkThatAssignmentDoesNotChangeImmutableValue(expr: Expression, messages: MutableList<Message>) {
    if (expr is Assignment) {
        val symbol = expr.definitionScope.getSymbol(expr.name)
        if (symbol?.mutable == false) {
            messages.add(CannotChangeImmutableVariable(expr.location))
        }
    }
}

fun checkThatFunctionHasAReturnValue(expr: Expression, messages: MutableList<Message>) {
    if (expr is Fn) {
        val expected = expr.returnType
        if (expr.body.body.isEmpty() && expected != Type.unit) {
            messages.add(MissingReturnValue(expected, expr.body.location))
        }
    }
}

fun checkThatFunctionCallsReceiveAppropriateCountOfArguments(expr: Expression, messages: MutableList<Message>) {
    if (expr is FnCall) {
        val valueType = expr.function.type

        if (valueType is FnType &&
            valueType.paramTypes.count() != expr.parameters.count()
        ) {
            messages.add(
                FunctionArityError(
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
        val valueType = expr.function.type

        if (valueType is OverloadedFnType) {
            val argumentTypes = expr.parameters.map { it.type }
            val candidates = valueType.types.count { it.paramTypes == argumentTypes }
            when {
                candidates == 0 -> messages.add(NoCandidatesForFunction(argumentTypes, expr.location))
                candidates >= 2 -> TODO("Ambigious definition for ${expr.function}")
            }
        }
    }
}

fun checkThatFunctionCallsActuallyCallFunctions(expr: Expression, messages: MutableList<Message>) {
    if (expr is FnCall) {
        val valueType = expr.function.type

        if (valueType !is FnType && valueType !is OverloadedFnType) {
            messages.add(NotAFunction(expr.location))
        }
    }
}

fun checkGenericTypes(expr: Expression, messages: MutableList<Message>) {
    if (expr is FnCall && expr.function.type is FnType && expr.callTypeParameters.isNotEmpty()) {
        val fnType = expr.function.type as FnType
        // check that all generic type parameters were passed
        if (fnType.genericTypeParameters.size != expr.callTypeParameters.size) {
            messages.add(
                GenericTypeArityError(
                    fnType.genericTypeParameters.size,
                    expr.callTypeParameters.size,
                    expr.function.location
                )
            )
        }

        // check that parameters passed to the function have the same type that is declared in generic type parameters
        val typeParameterNameToParamIndex =
            fnType.paramTypes.foldIndexed(mutableListOf<Pair<String, Int>>()) { paramIndex, acc, type ->
                if (type is GenericTypeParameter) {
                    acc.add(type.typeParameterName to paramIndex)
                }
                acc
            }.toMap()

        fnType.genericTypeParameters.forEachIndexed { genericTypeParameterIndex, genericTypeParameter ->
            val genericParamIndex = typeParameterNameToParamIndex[genericTypeParameter.typeParameterName]
            val genericParam = genericParamIndex?.let { expr.parameters[genericParamIndex] }
                ?: TODO("I'm not sure if this should happen. I suspect it shouldn't")
            val expectedType = expr.callTypeParameters[genericTypeParameterIndex]
            val actualType = genericParam.type
            if (actualType != expectedType) {
                messages.add(TypeMismatch(expectedType, actualType, expr.parameters[genericParamIndex].location))
            }
        }
    }
}

fun checkThatIfElseBranchTypesMatch(expr: Expression, messages: MutableList<Message>) {
    if (expr is IfElse) {
        val thenBlockType = expr.thenBranch.type
        val elseBlockType = expr.elseBranch?.type

        if (elseBlockType != null && thenBlockType != elseBlockType) {
            messages.add(IfElseBranchesTypeMismatch(thenBlockType, elseBlockType, expr.location))
        }
    }
}

fun checkTypes(expr: Expression, messages: MutableList<Message>) {

    fun checkTypeMatches(
        expected: Type,
        actual: Type,
        location: Location?,
        acceptAnyTypeAsGenericTypeParameter: Boolean = false
    ) {
        if (acceptAnyTypeAsGenericTypeParameter && expected.isGenericType()) {
            // accept any type
            return
        }
        val typeMatches = expected == actual || isSubType(actual, expected)
        if (!typeMatches) {
            messages.add(TypeMismatch(expected, actual, location))
        }
    }

    fun checkPrefixOp(op: PrefixOp) {
        when (op.op) {
            "!" -> if (op.expr.type != Type.bool) {
                messages.add(TypeMismatch(Type.bool, op.expr.type, op.location))
            }
            else -> TODO("Unimplemented prefix operator")
        }
    }

    fun checkAssignment(expr: Assignment) {
        val scope = expr.definitionScope

        val expectedType = scope.getSymbolType(expr.name)

        if (expectedType != null) {
            checkTypeMatches(expectedType, expr.value.type, expr.location)
        }
    }

    fun checkNameDeclaration(expr: NameDeclaration) {
        if (expr.expectedType != null) {
            checkTypeMatches(expr.expectedType, expr.value.type, expr.value.location)
        }
    }

    fun checkFn(expr: Fn) {
        val expected = expr.returnType
        if (expr.returnType == Type.unit) {
            return
        }

        if (expr.body.body.isNotEmpty()) {
            val actual = expr.body.type
            val location = expr.body.body.last().location
            checkTypeMatches(expected, actual, location, false)
        }
    }

    fun checkFnCall(expr: FnCall) {
        val valueType = expr.function.type

        if (valueType is FnType) {
            valueType.paramTypes.zip(expr.parameters) { definition, passed ->
                val actualType = passed.type
                checkTypeMatches(definition, actualType, passed.location, acceptAnyTypeAsGenericTypeParameter = true)
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
        } else if (expr.op in arrayOf("|", "&", "<<", ">>") && !leftType.isNumber()) {
            messages.add(TypeMismatch(expected = Type.intType, leftType, expr.left.location))
        } else if (expr.op in arrayOf("|", "&", "<<", ">>") && !rightType.isNumber()) {
            messages.add(TypeMismatch(expected = Type.intType, rightType, expr.right.location))
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

    fun checkWhileLoop(expr: WhileLoop) {
        checkTypeMatches(Type.bool, expr.condition.type, expr.location)
    }

    fun checkIndexOperator(expr: IndexOperator) {
        if (expr.variable.type.isIndexable()) {
            checkTypeMatches(expr.variable.type.expectedIndexType()!!, expr.index.type, expr.index.location)
        } else {
            messages.add(TypeIsNotIndexable(expr.variable.type, expr.variable.location))
        }
    }

    fun checkIndexedAssignment(expr: IndexedAssignment) {
        if (expr.variable.type.isIndexable()) {
            checkTypeMatches(expr.variable.type.expectedIndexType()!!, expr.index.type, expr.index.location)
            checkTypeMatches(expr.variable.type.indexedElementType()!!, expr.value.type, expr.value.location)
        } else {
            messages.add(TypeIsNotIndexable(expr.variable.type, expr.variable.location))
        }
    }

    @Suppress("UNUSED_VARIABLE")
    val ignored: Any = when (expr) {
        is Program -> {} // nothing to check
        is Package -> {} // nothing to check
        is Import -> {} // nothing to check
        is DefineComplexType -> {} // nothing to check
        is Assignment -> checkAssignment(expr)
        is NameDeclaration -> checkNameDeclaration(expr)
        is Block -> {} // nothing to check
        is Fn -> checkFn(expr)
        is FnCall -> checkFnCall(expr)
        is Atom -> {} // nothing to check
        is VariableAccess -> {} // nothing to check
        is FieldAccess -> {} // nothing to check
        is IfElse -> checkIfElseType(expr)
        is InfixOp -> checkInfixOp(expr)
        is PrefixOp -> checkPrefixOp(expr)
        is Cast -> checkCast(expr)
        is Group -> {} // nothing to check
        is WhileLoop -> checkWhileLoop(expr)
        is IndexOperator -> checkIndexOperator(expr)
        is IndexedAssignment -> checkIndexedAssignment(expr)
    }
}

private var typeGraph: Graph<String, DefaultEdge> =
    DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java).also {
        it.addVertex("unit")
        it.addVertex("int")
        it.addVertex("float")

        it.addEdge("int", "float")
        it.addEdge("int", "unit")
        it.addEdge("float", "unit")
    }

fun isSubType(subtype: Type, supertype: Type): Boolean {
    return if (subtype is ComplexTypeVariant) {
        return supertype == subtype.baseType
    } else if (subtype != supertype && typeGraph.containsVertex(subtype.name) && typeGraph.containsVertex(supertype.name)) {
        val dijkstraAlgo = DijkstraShortestPath(typeGraph)
        val path = dijkstraAlgo.getPath(subtype.name, supertype.name)
        path != null
    } else {
        false
    }
}

