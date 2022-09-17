package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import gh.marad.chi.core.parser.ChiSource
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

fun checkModuleAndPackageNames(expr: Expression, messages: MutableList<Message>) {
    if (expr is Package) {
        if (expr.moduleName.isEmpty()) {
            messages.add(InvalidModuleName(expr.moduleName, expr.sourceSection.toCodePoint()))
        }
        if (expr.packageName.isEmpty()) {
            messages.add(InvalidPackageName(expr.packageName, expr.sourceSection.toCodePoint()))
        }
    }
}

fun checkImports(expr: Expression, messages: MutableList<Message>) {
    if (expr is Import) {
        if (expr.moduleName.isEmpty()) {
            messages.add(InvalidModuleName(expr.moduleName, expr.sourceSection.toCodePoint()))
        }
        if (expr.packageName.isEmpty()) {
            messages.add(InvalidPackageName(expr.packageName, expr.sourceSection.toCodePoint()))
        }
    }
}

fun checkThatTypesContainAccessedMembers(expr: Expression, messages: MutableList<Message>) {
    if (expr is FieldAccess && expr.receiver.type.isCompositeType()) {
        val hasMember = (expr.receiver.type as CompositeType).hasMember(expr.fieldName)
        if (!hasMember) {
            messages.add(MemberDoesNotExist(expr.receiver.type, expr.fieldName, expr.memberSection.toCodePoint()))
        }
    }
}

fun checkThatVariableIsDefined(expr: Expression, messages: MutableList<Message>) {
    if (expr is VariableAccess) {
        if (!expr.definitionScope.containsSymbol(expr.name)) {
            messages.add(UnrecognizedName(expr.name, expr.sourceSection.toCodePoint()))
        }
    }
}

fun checkThatAssignmentDoesNotChangeImmutableValue(expr: Expression, messages: MutableList<Message>) {
    if (expr is Assignment) {
        val symbol = expr.definitionScope.getSymbol(expr.name)
        if (symbol?.mutable == false) {
            messages.add(CannotChangeImmutableVariable(expr.sourceSection.toCodePoint()))
        }
    }
}

fun checkThatFunctionHasAReturnValue(expr: Expression, messages: MutableList<Message>) {
    if (expr is Fn) {
        val expected = expr.returnType
        if (expr.body.body.isEmpty() && expected != Type.unit) {
            messages.add(MissingReturnValue(expected, expr.body.sourceSection.toCodePoint()))
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
                    expr.sourceSection.toCodePoint()
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
            if (valueType.getType(argumentTypes) == null) {
                messages.add(
                    NoCandidatesForFunction(
                        argumentTypes,
                        valueType.types.map { it.fnType }.toSet(),
                        expr.sourceSection.toCodePoint()
                    )
                )
            }
        }
    }
}

fun checkThatFunctionCallsActuallyCallFunctions(expr: Expression, messages: MutableList<Message>) {
    if (expr is FnCall) {
        val valueType = expr.function.type

        if (valueType !is FnType && valueType !is OverloadedFnType) {
            messages.add(NotAFunction(expr.sourceSection.toCodePoint()))
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
                    expr.function.sourceSection.toCodePoint()
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
                ?: return@forEachIndexed
            val expectedType = expr.callTypeParameters[genericTypeParameterIndex]
            val actualType = genericParam.type
            if (!typesMatch(expectedType, actualType)) {
                messages.add(
                    TypeMismatch(
                        expectedType,
                        actualType,
                        expr.parameters[genericParamIndex].sourceSection.toCodePoint()
                    )
                )
            }
        }
    }
}

fun typesMatch(
    expected: Type,
    actual: Type,
): Boolean {
    if (expected == Type.any) {
        // accept any type
        return true
    }
    return expected == actual || isSubType(actual, expected) || matchStructurally(
        expected,
        actual
    )
}

fun matchStructurally(expected: Type, actual: Type): Boolean {
    val expectedSubtypes = expected.getAllSubtypes()
    val actualSubtypes = actual.getAllSubtypes()
    return expected.javaClass == actual.javaClass &&
            expectedSubtypes.size == actualSubtypes.size &&
            expectedSubtypes.zip(actualSubtypes)
                .all { typesMatch(it.first, it.second) }
}


fun checkTypes(expr: Expression, messages: MutableList<Message>) {

    fun checkTypeMatches(
        expected: Type,
        actual: Type,
        sourceSection: ChiSource.Section?,
    ) {
        if (!typesMatch(expected, actual)) {
            messages.add(TypeMismatch(expected, actual, sourceSection.toCodePoint()))
        }
    }

    fun checkPrefixOp(op: PrefixOp) {
        when (op.op) {
            "!" -> if (op.expr.type != Type.bool) {
                messages.add(TypeMismatch(Type.bool, op.expr.type, op.sourceSection.toCodePoint()))
            }
            else -> TODO("Unimplemented prefix operator")
        }
    }

    fun checkAssignment(expr: Assignment) {
        val scope = expr.definitionScope

        val expectedType = scope.getSymbolType(expr.name)

        if (expectedType != null) {
            checkTypeMatches(expectedType, expr.value.type, expr.sourceSection)
        }
    }

    fun checkNameDeclaration(expr: NameDeclaration) {
        if (expr.expectedType != null) {
            checkTypeMatches(expr.expectedType, expr.value.type, expr.value.sourceSection)
        }
    }

    fun checkFn(expr: Fn) {
        val expected = expr.returnType
        if (expr.returnType == Type.unit) {
            return
        }

        if (expr.body.body.isNotEmpty()) {
            val actual = expr.body.type
            val sourceSection = expr.body.body.last().sourceSection
            checkTypeMatches(expected, actual, sourceSection)
        }
    }

    fun checkFnCall(expr: FnCall) {
        val fnType = expr.function.type

        if (fnType is FnType) {
            val genericParamToTypeFromPassedParameters =
                matchCallTypes(
                    fnType.paramTypes,
                    expr.parameters.map { it.type })
            fnType.paramTypes.zip(expr.parameters) { definition, passed ->
                val actualType = passed.type
                checkTypeMatches(
                    definition.construct(genericParamToTypeFromPassedParameters),
                    actualType,
                    passed.sourceSection
                )
            }

            if (expr.callTypeParameters.isNotEmpty()) {
                val genericParamToTypeFromDefinedParameters =
                    matchTypeParameters(fnType.genericTypeParameters, expr.callTypeParameters)
                fnType.genericTypeParameters.forEach { param ->
                    val expected = genericParamToTypeFromDefinedParameters[param]!!
                    val actual = genericParamToTypeFromPassedParameters[param]
                    if (actual != null && !typesMatch(expected, actual)) {
                        messages.add(GenericTypeMismatch(expected, actual, param, expr.sourceSection.toCodePoint()))
                    }
                }
            }
        }
    }

    fun checkFieldAssignment(expr: FieldAssignment) {
        val memberType = (expr.receiver.type as CompositeType).memberType(expr.fieldName)!!
        val assignedType = expr.value.type
        checkTypeMatches(expected = memberType, actual = assignedType, expr.value.sourceSection)
    }

    fun checkIfElseType(expr: IfElse) {
        val conditionType = expr.condition.type
        if (conditionType != Type.bool) {
            messages.add(TypeMismatch(Type.bool, conditionType, expr.condition.sourceSection.toCodePoint()))
        }
    }

    fun checkInfixOp(expr: InfixOp) {
        val leftType = expr.left.type
        val rightType = expr.right.type

        if (leftType != rightType) {
            messages.add(TypeMismatch(expected = leftType, rightType, expr.right.sourceSection.toCodePoint()))
        } else if (expr.op in arrayOf("|", "&", "<<", ">>") && !leftType.isNumber()) {
            messages.add(TypeMismatch(expected = Type.intType, leftType, expr.left.sourceSection.toCodePoint()))
        } else if (expr.op in arrayOf("|", "&", "<<", ">>") && !rightType.isNumber()) {
            messages.add(TypeMismatch(expected = Type.intType, rightType, expr.right.sourceSection.toCodePoint()))
        }
    }

    fun checkCast(expr: Cast) {
    }

    fun checkWhileLoop(expr: WhileLoop) {
        checkTypeMatches(Type.bool, expr.condition.type, expr.sourceSection)
    }

    fun checkIndexOperator(expr: IndexOperator) {
        if (expr.variable.type.isIndexable()) {
            checkTypeMatches(expr.variable.type.expectedIndexType(), expr.index.type, expr.index.sourceSection)
        } else {
            messages.add(TypeIsNotIndexable(expr.variable.type, expr.variable.sourceSection.toCodePoint()))
        }
    }

    fun checkIndexedAssignment(expr: IndexedAssignment) {
        if (expr.variable.type.isIndexable()) {
            checkTypeMatches(expr.variable.type.expectedIndexType(), expr.index.type, expr.index.sourceSection)
            checkTypeMatches(expr.variable.type.indexedElementType(), expr.value.type, expr.value.sourceSection)
        } else {
            messages.add(TypeIsNotIndexable(expr.variable.type, expr.variable.sourceSection.toCodePoint()))
        }
    }

    fun checkIs(expr: Is) {
    }

    @Suppress("UNUSED_VARIABLE")
    val ignored: Any = when (expr) {
        is Program -> {} // nothing to check
        is Package -> {} // nothing to check
        is Import -> {} // nothing to check
        is DefineVariantType -> {} // nothing to check
        is Assignment -> checkAssignment(expr)
        is NameDeclaration -> checkNameDeclaration(expr)
        is Block -> {} // nothing to check
        is Fn -> checkFn(expr)
        is FnCall -> checkFnCall(expr)
        is Atom -> {} // nothing to check
        is VariableAccess -> {} // nothing to check
        is FieldAccess -> {} // nothing to check
        is FieldAssignment -> checkFieldAssignment(expr)
        is IfElse -> checkIfElseType(expr)
        is InfixOp -> checkInfixOp(expr)
        is PrefixOp -> checkPrefixOp(expr)
        is Cast -> checkCast(expr)
        is Group -> {} // nothing to check
        is WhileLoop -> checkWhileLoop(expr)
        is IndexOperator -> checkIndexOperator(expr)
        is IndexedAssignment -> checkIndexedAssignment(expr)
        is Is -> checkIs(expr)
        is Break -> {} // nothing to check
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
    return if (subtype != supertype && typeGraph.containsVertex(subtype.name) && typeGraph.containsVertex(supertype.name)) {
        val dijkstraAlgo = DijkstraShortestPath(typeGraph)
        val path = dijkstraAlgo.getPath(subtype.name, supertype.name)
        path != null
    } else {
        false
    }
}

