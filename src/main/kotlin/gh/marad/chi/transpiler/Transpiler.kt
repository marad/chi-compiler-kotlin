package gh.marad.chi.transpiler

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.Scope
import gh.marad.chi.core.analyzer.inferType
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

fun main() {
    val code = Files.readString(Paths.get("test.chi"))
    val cCode = transpile(code)
    Files.write(Paths.get("test.c"), cCode.toByteArray())
    "gcc test.c".runCommand(File("."))
    "./a.exe".runCommand(File("."))
}

fun transpile(code: String): String {
    val scope = Scope()
    val result = StringBuilder()
    result.append("#include <stdio.h>\n")
    Prelude.init(scope, result)

    val compilationResult = compile(code, scope)

    val emitter = Emitter()
    compilationResult.messages.forEach { println(it) }
    if (compilationResult.hasErrors()) {
        throw RuntimeException("There were compilation errors.")
    }

    compilationResult.ast.forEach { emitter.emit(compilationResult.scope, it) }

    result.append(emitter.getCode())
    result.append('\n')
    return result.toString()
}

object Prelude {
    fun init(scope: Scope, sb: StringBuilder) {
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

    fun emit(scope: Scope, expr: Expression) {
        when(expr) {
            is Atom -> outputAtom(expr)
            is NameDeclaration -> outputNameDeclaration(scope, expr)
            is BlockExpression -> throw UnsupportedOperationException()
            is Fn -> throw UnsupportedOperationException()
            is FnCall -> outputFunctionCall(scope, expr)
            is VariableAccess -> sb.append(expr.name)
        }
    }

    private fun outputNameDeclaration(scope: Scope, expr: NameDeclaration) {
        if (expr.value is Fn) {
            outputFunctionDeclaration(scope, expr)
        } else {
            outputVariableDeclaration(scope, expr)
        }
    }

    private fun outputFunctionDeclaration(scope: Scope, expr: NameDeclaration) {
        // TODO: inlined functions should be declared before (probably with fixed names to avoid collisions)
        val fn = expr.value as Fn
        val subscope = Scope.fromExpressions(fn.block.body, scope)

        outputType(fn.returnType)
        sb.append(' ')
        sb.append(expr.name)
        sb.append('(')
        fn.parameters.dropLast(1).forEach {
            outputType(it.type)
            sb.append(' ')
            sb.append(it.name)
            sb.append(", ")
        }
        if (fn.parameters.isNotEmpty()) {
            val it = fn.parameters.last()
            outputType(it.type)
            sb.append(' ')
            sb.append(it.name)
        }
        sb.append(')')
        sb.append(" {\n")
        outputFunctionBody(subscope, fn)
        sb.append("}\n")
    }

    private fun outputVariableDeclaration(scope: Scope, expr: NameDeclaration) {
        outputType(inferType(scope, expr))
        sb.append(' ')
        sb.append(expr.name)
        sb.append(" = ")
        emit(scope, expr.value)
        sb.append(';')
        sb.append('\n')
    }

    private fun outputFunctionBody(scope: Scope, fn: Fn) {
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

    private fun outputFunctionCall(scope: Scope, expr: FnCall) {
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

    private fun outputAtom(expr: Atom) {
        sb.append(expr.value)
    }

    private fun outputType(type: Type) {
        when(type) {
            Type.i32 -> sb.append("int")
            Type.unit -> sb.append("void")
            is FnType -> sb.append("void *")
        }

    }
}