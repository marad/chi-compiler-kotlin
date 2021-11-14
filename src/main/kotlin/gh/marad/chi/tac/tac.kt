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
data class TacFunction(override val name: String, override val type: Type, val returnType: Type, val params: List<FnParam>, val body: List<Tac>) : Tac {
    init { assert(type is FnType) { "type should be function type"} }
}
data class TacCall(override val name: String, override val type: Type, val parameters: List<Operand>) : Tac
data class TacReturn(override val name: String, override val type: Type, val retVal: Operand) : Tac
data class TacIfElse(override val name: String, override val type: Type, val condition: Operand, val thenBranch: List<Tac>, val elseBranch: List<Tac>?) : Tac


private class TacEmitter {
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
        val body = mutableListOf<Tac>()
        body.addAll(fn.block.body.flatMap { emitExpression(it) })
        if (fn.returnType != Type.unit) {
            val lastTac = body.last()
            body.add(TacReturn("", lastTac.type, TacName(lastTac.name)))
        }
        return listOf(
            TacFunction(name, fn.type, fn.returnType, fn.parameters, body)
        )
    }

    private fun emitFnCall(fnCall: FnCall): List<Tac> {
        val result = mutableListOf<Tac>()
        val parameters = mutableListOf<Operand>()
        fnCall.parameters.forEach {
            val exprTac = emitExpression(it)
            result.addAll(exprTac)
            parameters.add(TacName(exprTac.last().name))
        }
        result.add(TacCall(fnCall.name, inferType(fnCall), parameters))
        return result
    }

    private fun emitIfElse(ifElse: IfElse): List<Tac> {
        val result = mutableListOf<Tac>()
        val condition = emitExpression(ifElse.condition)
        val ifResultTmpName = nextTmpName()
        val type = inferType(ifElse)
        // TODO: handling anonymous functions like in emitFunctionWithName - probably should extract `Block` emitting
        val thenBranch = ifElse.thenBranch.body.flatMap { emitExpression(it) }.toMutableList().also {
            it.add(TacAssignment(ifResultTmpName, type, TacName(it.last().name)))
        }
        val elseBranch = ifElse.elseBranch?.body?.flatMap { emitExpression(it) }?.toMutableList()?.also {
            it.add(TacAssignment(ifResultTmpName, type, TacName(it.last().name)))
        }

        result.addAll(condition)
        result.add(TacDeclaration(ifResultTmpName, type))
        result.add(TacIfElse(
            ifResultTmpName,
            type,
            TacName(condition.last().name),
            thenBranch,
            elseBranch
        ))
        return result
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

fun emitCType(type: Type): String {
    return when(type) {
        Type.i32 -> "int"
        Type.bool -> "bool"
        Type.unit -> "void"
        else -> throw RuntimeException("Unsupported type ${type}!")
    }
}

fun emitC(tac: List<Tac>): String {
    val sb = StringBuilder()
    tac.forEach {
        val ignored = when(it) {
            is TacAssignment -> sb.append("${it.name} = ${emitOperand(it.value)};\n")
            is TacAssignmentOp -> sb.append("${emitCType(it.type)} ${it.name} = ${emitOperand(it.a)} ${it.op} ${emitOperand(it.b)};\n")
            is TacCall -> {
                sb.append("${it.name}(")
                sb.append(it.parameters.joinToString(",") { param -> emitOperand(param)})
                sb.append(");\n");
            }
            is TacDeclaration -> {
                sb.append(emitCType(it.type))
                if (it.value == null) {
                    sb.append(" ${it.name};\n")
                } else {
                    sb.append(" ${it.name} = ${emitOperand(it.value)};\n")
                }
            }
            is TacFunction -> {
                sb.append(emitCType(it.returnType))
                sb.append(" ${it.name}(")
                sb.append(
                    it.params.joinToString(",") { param ->
                        "${emitCType(param.type)} ${param.name}"
                    }
                )
                sb.append(") {\n")
                sb.append(emitC(it.body))
                sb.append("}\n")
            }
            is TacReturn -> "return ${emitOperand(it.retVal)}"
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
            val a = val b = 6
            val x = if (false) {
                10
            } else {
                b
            }
            
            println(x+2)
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
