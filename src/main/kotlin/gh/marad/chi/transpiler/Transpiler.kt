package gh.marad.chi.transpiler

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.Scope
import gh.marad.chi.core.analyzer.inferType
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
        val cCode = time("transpile") { transpile(code) }
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
    val scope = Scope<Expression>()
    val result = StringBuilder()
    result.append("#include <stdio.h>\n")
    Prelude.init(scope, result)

    val compilationResult = compile(code, scope)

    val emitter = Emitter()
    compilationResult.messages.forEach { System.err.println(it.message) }
    if (compilationResult.hasErrors()) {
        throw RuntimeException("There were compilation errors.")
    }

    emitter.emit(compilationResult.scope, compilationResult.ast)

    result.append(emitter.getCode())
    result.append('\n')
    return result.toString()
}

object Prelude {
    fun init(scope: Scope<Expression>, sb: StringBuilder) {
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

    fun emit(scope: Scope<Expression>, exprs: List<Expression>) {
        exprs.forEach {
            emit(scope, it)
            if (it is NameDeclaration && it.value !is Fn) {
                sb.append(";\n")
            }
        }
    }

    fun emit(scope: Scope<Expression>, expr: Expression) {
        // this val here is so that `when` give error instead of warn on non-exhaustive match
        val ignored: Any = when(expr) {
            is Atom -> emitAtom(expr)
            is NameDeclaration -> emitNameDeclaration(scope, expr)
            is Block -> emitBlock(scope, expr)
            is Fn -> throw UnsupportedOperationException()
            is FnCall -> emitFunctionCall(scope, expr)
            is VariableAccess -> sb.append(expr.name)
            is Assignment -> emitAssignment(scope, expr)
            is IfElse -> emitIfElse(scope, expr)
        }
    }

    private fun emitAssignment(scope: Scope<Expression>, assignment: Assignment) {
        sb.append(assignment.name)
        sb.append('=')
        emit(scope, assignment.value)
    }

    private fun emitNameDeclaration(scope: Scope<Expression>, expr: NameDeclaration) {
        if (expr.value is Fn) {
            outputFunctionDeclaration(scope, expr)
        } else {
            emitVariableDeclaration(scope, expr)
        }
    }

    private fun outputFunctionDeclaration(scope: Scope<Expression>, expr: NameDeclaration) {
        // TODO: inlined functions should be declared before (probably with fixed names to avoid collisions)
        val fn = expr.value as Fn
        val subscope = Scope.fromExpressions(fn.block.body, scope)

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
        outputFunctionBody(subscope, fn)
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

    private fun emitVariableDeclaration(scope: Scope<Expression>, expr: NameDeclaration) {
        val outputType = inferType(scope, expr)
        emitNameAndType(expr.name, outputType)
        sb.append(" = ")
        emit(scope, expr.value)
    }

    private fun emitType(type: Type) {
        when(type) {
            Type.i32 -> sb.append("int")
            Type.unit -> sb.append("void")
            is FnType -> sb.append("THIS FUNCTION SHOULD NOT BE USED WITH FnType")
            else -> TODO()
        }

    }

    private fun outputFunctionBody(scope: Scope<Expression>, fn: Fn) {
        // function declarations should be removed (as they should be handled before)
        // or maybe just make them invalid by language rules?
        // last emit should be prepended by 'return'
        val body = fn.block.body
        body.dropLast(1).forEach {
            emit(scope, it)
            sb.append(";\n")
        }
        if (body.isNotEmpty()) {
            val it = body.last()
            if (fn.returnType != Type.unit) {
                sb.append("return ")
            }
            emit(scope, it)
            sb.append(";\n")
        }
    }

    private fun emitFunctionCall(scope: Scope<Expression>, expr: FnCall) {
        sb.append(expr.name)
        sb.append('(')
        expr.parameters.dropLast(1).forEach {
            emit(scope, it)
            sb.append(", ")
        }
        if (expr.parameters.isNotEmpty()) {
            emit(scope, expr.parameters.last())
        }
        sb.append(")")
    }

    private fun emitAtom(expr: Atom) {
        sb.append(expr.value)
    }

    private fun emitBlock(scope: Scope<Expression>, expr: Block) {
        sb.append("{")
        expr.body.forEach {
            emit(scope, it)
            sb.append(';')
        }
        sb.append("}")
    }

    private fun emitIfElse(scope: Scope<Expression>, expr: IfElse) {
        // TODO: if-else should be expression (will probably require some tmp variable and
        //  setting it as the last operation of each branch to value of the last expression)
        //  so 'val x = if(true) { 1 } else { 2 }' becomes
        //  int x;
        //  if (...) { x = 1 } else { x = 2 }
        sb.append("if(")
        emit(scope, expr.condition)
        sb.append(")")
        emit(scope, expr.thenBranch)
        if (expr.elseBranch != null) {
            sb.append("else")
            emit(scope, expr.elseBranch)
        }
    }
}
