package gh.marad.chi.transpiler

import gh.marad.chi.actionast.*
import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.compile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun <T> time(comment: String, f: () -> T): T {
    val start = System.nanoTime()
    val result = f()
    val end = System.nanoTime()
    val duration = (end-start) / 1000_000
    println("$comment - $duration ms")
    return result
}

fun main() {
    val code = Files.readString(Paths.get("test.chi"))
    try {
        val cCode = time("total") { transpile(code) }
        Files.write(Paths.get("test.c"), cCode.toByteArray())
        "gcc test.c".runCommand(File("."))
        "./a.exe".runCommand(File("."))
    } catch (ex: RuntimeException) {
        ex.printStackTrace()
    }
}

private fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

fun transpile(code: String): String {
    val result = StringBuilder()
    result.append("#include <stdio.h>\n")

    val compilationScope = CompilationScope()
    time("init") {
        Prelude.init(compilationScope, result)
    }
    val compilationResult = time("compilation") {
        compile(code, compilationScope)
    }

    val emitter = Emitter()
    compilationResult.messages.forEach { System.err.println(it.message) }
    if (compilationResult.hasErrors()) {
        throw RuntimeException("There were compilation errors.")
    }

    time("emitting") {
        emitter.emit(compilationResult.ast)
    }

    result.append(emitter.getCode())
    result.append('\n')
    return time("building stirng") { result.toString() }
}

object Prelude {
    fun init(scope: CompilationScope, sb: StringBuilder) {
        scope.defineExternalName("println", Type.fn(Type.unit, Type.i32))
        sb.append("""
            void println(int i) {
              printf("%d\n", i);
            }
        """.trimIndent())
        sb.append('\n')
    }
}

class Emitter {
    private val sb = StringBuilder()

    fun getCode(): String = sb.toString()

    fun emit(exprs: List<ActionAst>) {
        exprs.forEach {
            emit(it)
            if (it is NameDeclaration && it.value !is Fn) {
                sb.append(";\n")
            }
        }
    }

    fun emit(expr: ActionAst) {
        // this val here is so that `when` give error instead of warn on non-exhaustive match
        val ignored: Any = when(expr) {
            is Atom -> emitAtom(expr)
            is NameDeclaration -> emitNameDeclaration(expr)
            is Block -> emitBlock(expr)
            is Fn -> throw UnsupportedOperationException()
            is FnCall -> emitFunctionCall(expr)
            is VariableAccess -> sb.append(expr.name)
            is Assignment -> emitAssignment(expr)
            is IfElse -> emitIfElse(expr)
            is InfixOp -> TODO()
        }
    }

    private fun emitAssignment(assignment: Assignment) {
        sb.append(assignment.name)
        sb.append('=')
        emit(assignment.value)
    }

    private fun emitNameDeclaration(expr: NameDeclaration) {
        if (expr.value is Fn) {
            outputFunctionDeclaration(expr)
        } else {
            emitVariableDeclaration(expr)
        }
    }

    private fun outputFunctionDeclaration(expr: NameDeclaration) {
        // TODO: inlined functions should be declared before (probably with fixed names to avoid collisions)
        val fn = expr.value as Fn

        emitType(fn.returnType)
        sb.append(' ')
        sb.append(expr.name)
        sb.append('(')
        fn.parameters.dropLast(1).forEach {
            emitNameAndType(it.name, it.type)
            sb.append(", ")
        }
        if (fn.parameters.isNotEmpty()) {
            val it = fn.parameters.last()
            emitNameAndType(it.name, it.type)
        }
        sb.append(')')
        sb.append(" {\n")
        outputFunctionBody(fn)
        sb.append("}\n")
    }

    private fun emitNameAndType(name: String, type: Type) {
        if (type is FnType) {
            emitType(type.returnType)
            sb.append(" (*")
            sb.append(name)
            sb.append(")")
            sb.append('(')
            type.paramTypes.dropLast(1).forEach {
                emitType(it)
                sb.append(',')
            }
            if (type.paramTypes.isNotEmpty()) {
                emitType(type.paramTypes.last())
            }
            sb.append(')')
        } else {
            emitType(type)
            sb.append(' ')
            sb.append(name)
        }
    }

    private fun emitVariableDeclaration(expr: NameDeclaration) {
        emitNameAndType(expr.name, expr.type)
        sb.append(" = ")
        emit(expr.value)
    }

    private fun emitType(type: Type) {
        when(type) {
            Type.i32 -> sb.append("int")
            Type.unit -> sb.append("void")
            is FnType -> sb.append("THIS FUNCTION SHOULD NOT BE USED WITH FnType")
            else -> TODO()
        }

    }

    private fun outputFunctionBody(fn: Fn) {
        // function declarations should be removed (as they should be handled before)
        // or maybe just make them invalid by language rules?
        // last emit should be prepended by 'return'
        val body = fn.block.body
        body.dropLast(1).forEach {
            emit(it)
            sb.append(";\n")
        }
        if (body.isNotEmpty()) {
            val it = body.last()
            if (fn.returnType != Type.unit) {
                sb.append("return ")
            }
            emit(it)
            sb.append(";\n")
        }
    }

    private fun emitFunctionCall(expr: FnCall) {
        sb.append(expr.name)
        sb.append('(')
        expr.parameters.dropLast(1).forEach {
            emit(it)
            sb.append(", ")
        }
        if (expr.parameters.isNotEmpty()) {
            emit(expr.parameters.last())
        }
        sb.append(")")
    }

    private fun emitAtom(expr: Atom) {
        sb.append(expr.value)
    }

    private fun emitBlock(expr: Block) {
        sb.append("{")
        expr.body.forEach {
            emit(it)
            sb.append(';')
        }
        sb.append("}")
    }

    private fun emitIfElse(expr: IfElse) {
        // TODO: if-else should be expression (will probably require some tmp variable and
        //  setting it as the last operation of each branch to value of the last expression)
        //  so 'val x = if(true) { 1 } else { 2 }' becomes
        //  int x;
        //  if (...) { x = 1 } else { x = 2 }
        sb.append("if(")
        emit(expr.condition)
        sb.append(")")
        emit(expr.thenBranch)
        if (expr.elseBranch != null) {
            sb.append("else")
            emit(expr.elseBranch)
        }
    }
}
