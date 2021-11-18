package gh.marad.chi.tac

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.inferType

sealed interface Operand
data class TacName(val name: String) : Operand
data class TacValue(val value: String) : Operand

sealed interface Tac {
    val name: String
    val type: Type
}
data class TacDeclaration(override val name: String, override val type: Type, val value: Operand? = null) : Tac
data class TacAssignment(override val name: String, override val type: Type, val value: Operand) : Tac
data class TacAssignmentOp(override val name: String, override val type: Type,
                           val a: Operand, val op: String, val b: Operand) : Tac
data class TacFunction(override val name: String, override val type: Type, val functionName: String, val paramNames: List<String>, val body: List<Tac>) : Tac {
    init {
        assert(type is FnType) { "type should be function type"}
        assert((type as FnType).paramTypes.size == paramNames.size) { "Parameter type and parameter name lists should have the same size" }
    }
    val returnType: Type = (type as FnType).returnType
    val paramTypes: List<Type> = (type as FnType).paramTypes
    val paramsWithTypes = paramNames.zip(paramTypes)
}
data class TacCall(override val name: String, override val type: Type, val functionName: String, val parameters: List<Operand>) : Tac
data class TacReturn(override val type: Type, val retVal: Operand) : Tac {
    override val name: String = ""
}
data class TacIfElse(override val name: String, override val type: Type, val condition: Operand, val thenBranch: List<Tac>, val elseBranch: List<Tac>?) : Tac
data class TacNot(override val name: String, val value: Operand) : Tac {
    override val type: Type = Type.bool
}


class TacEmitter {
    private var tmpCount = 0

    private fun nextTmpName(): String = "tmp$${tmpCount++}"

    fun emitProgram(program: Program): List<Tac> {
        return program.expressions.flatMap { emitExpression(it) }
    }

    private fun emitExpression(expr: Expression): List<Tac> {
        return when(expr) {
            is Atom -> emitAtom(expr)
            is VariableAccess -> emitVariableAccess(expr)
            is NameDeclaration -> emitNameDeclaration(expr)
            is Assignment -> emitAssignment(expr)
            is Fn -> emitAnonymousFn(expr)
            is Block -> throw NotImplementedError("Block should be handled by emitFn and should not be emitted directly")
            is FnCall -> emitFnCall(expr)
            is IfElse -> emitIfElse(expr)
            is InfixOp -> emitInfixOp(expr)
            is Program -> throw NotImplementedError("Program is top-level expression and should not be emitted directly")
            is PrefixOp -> when(expr.op) {
                "!" -> emitNotOperator(expr)
                else -> TODO("Unsupported prefix operator")
            }
        }
    }

    private fun emitNotOperator(expr: PrefixOp): List<Tac> {
        val result = mutableListOf<Tac>()
        result.addAll(emitExpression(expr.expr))
        result.add(TacNot(nextTmpName(), TacName(result.last().name)))
        return result
    }

    private fun emitAtom(atom: Atom): List<Tac> = emitAtomWithName(nextTmpName(), atom)

    private fun emitAtomWithName(name: String, atom: Atom): List<Tac> =
        listOf(TacDeclaration(name, inferType(atom), TacValue(atom.value)))
    private fun emitVariableAccess(varAcc: VariableAccess): List<Tac> = emitVariableAccessWithName(nextTmpName(), varAcc)


    private fun emitVariableAccessWithName(name: String, varAcc: VariableAccess): List<Tac> =
        listOf(TacDeclaration(name, inferType(varAcc), TacName(varAcc.name)))
    private fun emitNameDeclaration(expr: NameDeclaration): List<Tac> {
        return when (expr.value) {
            // TODO: this may not be necessary if replaced by later TAC optimizations
            is Fn -> emitFunctionWithName(expr.name, expr.value)
            is Atom -> emitAtomWithName(expr.name, expr.value)
            is VariableAccess -> emitVariableAccessWithName(expr.name, expr.value)
            else -> {
                val valueTac = emitExpression(expr.value)
                val result = mutableListOf<Tac>()
                result.addAll(valueTac)
                result.add(TacDeclaration(expr.name, inferType(expr), TacName(valueTac.last().name)))
                result
            }
        }
    }

    private fun emitAssignment(expr: Assignment): List<Tac> {
        val result = mutableListOf<Tac>()
        when (expr.value) {
            is Atom ->
                result.add(TacAssignment(expr.name, inferType(expr), TacValue(expr.value.value)))
            is VariableAccess ->
                result.add(TacAssignment(expr.name, inferType(expr), TacName(expr.value.name)))
            else -> {
                result.addAll(emitExpression(expr.value))
                result.add(TacAssignment(expr.name, inferType(expr), TacName(result.last().name)))
            }
        }
        return result
    }

    private fun emitAnonymousFn(fn: Fn): List<Tac> {
        return emitFunctionWithName(nextTmpName(), fn)
    }

    private fun emitFunctionWithName(name: String, fn: Fn): List<Tac> {
        val body = extractFunctions(fn.block.body.flatMap { emitExpression(it) })

        val result = mutableListOf<Tac>()

        result.addAll(body.innerFunctions)
        result.add(TacFunction(nextTmpName(), fn.type, name, fn.parameters.map { it.name }, body.sanitized.addReturnForNonUnitType(fn.returnType)))
        return result
    }

    private fun List<Tac>.addReturnForNonUnitType(type: Type): List<Tac> =
        if (type == Type.unit) {
            this
        } else {
            val lastTac = last()
            this + listOf(TacReturn(lastTac.type, TacName(lastTac.name)))
        }

    private data class FunctionExtractionResult(
        val innerFunctions: List<TacFunction>,
        val sanitized: List<Tac>,
    )

    private fun extractFunctions(body: List<Tac>): FunctionExtractionResult {
        // TODO: extracted functions should have some name part generated to avoid name collisions
        val innerFunctions = body.filterIsInstance<TacFunction>()
        val sanitizedBody = body.map {
            if (it is TacFunction) {
                TacDeclaration(nextTmpName(), it.type, TacName(it.functionName))
            } else {
                it
            }
        }
        return FunctionExtractionResult(innerFunctions, sanitizedBody)
    }

    private fun emitFnCall(fnCall: FnCall): List<Tac> {
        val result = mutableListOf<Tac>()
        val parameters = mutableListOf<Operand>()
        fnCall.parameters.forEach {
            val exprTac = emitExpression(it)
            result.addAll(exprTac)
            parameters.add(TacName(exprTac.last().name))
        }
        result.add(TacCall(nextTmpName(), inferType(fnCall), fnCall.name, parameters))
        return result
    }

    private fun emitIfElse(ifElse: IfElse): List<Tac> {
        val result = mutableListOf<Tac>()
        val condition = emitExpression(ifElse.condition)
        val ifResultTmpName = nextTmpName()
        val type = inferType(ifElse)

        val thenBranchS = ifElse.thenBranch.body
            .flatMap { emitExpression(it) }
            .let { extractFunctions(it) }

        val elseBranchS =ifElse.elseBranch?.body
            ?.flatMap { emitExpression(it) }
            ?.let { extractFunctions(it) }

        thenBranchS.innerFunctions.let(result::addAll)
        elseBranchS?.innerFunctions?.let(result::addAll)

        result.addAll(condition)
        if (type != Type.unit) {
            result.add(TacDeclaration(ifResultTmpName, type))
        }
        result.add(TacIfElse(
            ifResultTmpName,
            type,
            TacName(condition.last().name),
            thenBranchS.sanitized.addAssignmentForNonUnitType(ifResultTmpName, type),
            elseBranchS?.sanitized?.addAssignmentForNonUnitType(ifResultTmpName, type)
        ))
        return result
    }

    private fun List<Tac>.addAssignmentForNonUnitType(variableName: String, type: Type) =
        if(type != Type.unit) {
            this + listOf(TacAssignment(variableName, type, TacName(last().name)))
        } else {
            this
        }

    private fun emitInfixOp(expr: InfixOp): List<Tac> {
        val result = mutableListOf<Tac>()
        val left = emitExpression(expr.left)
        val right = emitExpression(expr.right)
        result.addAll(left)
        result.addAll(right)
        result.add(TacAssignmentOp(
            nextTmpName(),
            inferType(expr),
            TacName(left.last().name),
            expr.op,
            TacName(right.last().name)
        ))
        return result
    }
}
