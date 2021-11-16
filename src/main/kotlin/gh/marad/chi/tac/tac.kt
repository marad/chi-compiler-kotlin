package gh.marad.chi.tac

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.inferType
import gh.marad.chi.transpiler.runCommand
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


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
            is Block -> TODO("This should be handled by emitFn and should not be here directly")
            is FnCall -> emitFnCall(expr)
            is IfElse -> emitIfElse(expr)
            is InfixOp -> emitInfixOp(expr)
            is Program -> TODO("Program is top-level expression and should not be here directly")
        }

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
        // TODO Handle inner functions
        //      All `TacFunction` elements in `body` should be extracted before and switched to `TacCall`
        //      Should automatically add the outer scope names as arguments
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


fun emitOperand(operand: Operand): String = when(operand) {
    is TacName -> operand.name
    is TacValue -> operand.value
}

fun emitCTypeWithName(type: Type, name: String?): String {
    return when(type) {
        Type.i32 -> "int ${name?:""}"
        Type.bool -> "bool ${name?:""}"
        Type.unit -> "void ${name?:""}"
        is FnType -> {
            val params = type.paramTypes.joinToString(",") { emitCTypeWithName(it, null) }
            if (type.returnType is FnType) {
                val functionName = "(*${name?:""})($params)"
                emitCTypeWithName(type.returnType, functionName)
            } else {
                "${emitCTypeWithName(type.returnType, null)} (*${name?:""})($params)"
            }
        }
        else -> throw RuntimeException("Unsupported type ${type}!")
    }
}


fun emitC(tac: List<Tac>): String {
    val sb = StringBuilder()
    tac.forEach {
        val ignored = when(it) {
            is TacAssignment -> sb.append("${it.name} = ${emitOperand(it.value)};\n")
            is TacAssignmentOp -> sb.append("${emitCTypeWithName(it.type, it.name)} = ${emitOperand(it.a)} ${it.op} ${emitOperand(it.b)};\n")
            is TacCall -> {
                if (it.type != Type.unit) {
                    sb.append(emitCTypeWithName(it.type, it.name))
                    sb.append(" = ")
                }
                sb.append("${it.functionName}(")
                sb.append(it.parameters.joinToString(",") { param -> emitOperand(param)})
                sb.append(");\n");
            }
            is TacDeclaration -> {
                sb.append(emitCTypeWithName(it.type, it.name))
                if (it.value != null) {
                    sb.append(" = ${emitOperand(it.value)}")
                }
                sb.append(";\n")
            }
            is TacFunction -> {
                val functionNameWithArgs = StringBuilder()
                functionNameWithArgs.append(it.functionName)
                functionNameWithArgs.append(' ')
                functionNameWithArgs.append("(")
                functionNameWithArgs.append(
                    it.paramsWithTypes.joinToString(", ") { param ->
                        emitCTypeWithName(param.second, param.first)
                    }
                )
                functionNameWithArgs.append(")")

                sb.append(emitCTypeWithName(it.returnType, functionNameWithArgs.toString()))
                sb.append(" {\n")
                sb.append(emitC(it.body))
                sb.append("}\n")
            }
            is TacReturn -> sb.append("return ${emitOperand(it.retVal)};\n")
            is TacIfElse -> {
                sb.append("if(${emitOperand(it.condition)}) {\n")
                sb.append(emitC(it.thenBranch))
                if (it.elseBranch != null) {
                    sb.append("} else {\n")
                    sb.append(emitC(it.elseBranch))
                }
                sb.append("};\n")
            }
        }
    }

    return sb.toString()
}

fun main() {
    val compilationScope = CompilationScope()
    compilationScope.defineExternalName("println", Type.fn(Type.unit, Type.i32))
    val program = parseProgram("""
        val main = fn() {
            if (true) {
               val a = fn() {}
            } else {
               val b = fn() {}
            }
        }
    """.trimIndent(), compilationScope)

    val messages = analyze(program)

    if (messages.isNotEmpty()) {
        messages.forEach { println(it) }
        exitProcess(1)
    }

    val tac = TacEmitter().emitProgram(program)
    tac.forEach { println(it) }

    println()

    val cCode = """
        #include <stdio.h>
        #include <stdbool.h>
        
        void println(int i) {
          printf("%d\n", i);
        }
        
    """.trimIndent() + emitC(tac)
    println(cCode)
    Files.write(Paths.get("test.c"), cCode.toByteArray())

    println("GCC Compilation...")
    "gcc test.c".runCommand(File("."))
    println("Running...")
    "./a.exe".runCommand(File("."))
}
