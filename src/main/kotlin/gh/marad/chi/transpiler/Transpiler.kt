package gh.marad.chi.transpiler

import gh.marad.chi.core.*
import gh.marad.chi.tac.*
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

fun String.runCommand(workingDir: File) {
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
    result.append("#include <stdbool.h>\n")


    val compilationScope = CompilationScope()
    time("init") {
        Prelude.init(compilationScope, result)
    }
    val compilationResult = time("compilation") {
        compile(code, compilationScope)
    }

    compilationResult.messages.forEach { System.err.println(formatCompilationMessage(code, it)) }
    if (compilationResult.hasErrors()) {
        throw RuntimeException("There were compilation errors.")
    }

    val cCode = time("emitting") {
        CEmitter.emit(compilationResult.program)
    }

    result.append(cCode)
    result.append("int main() { chi\$main(); return 0; }")
    result.append('\n')
    return time("building string") { result.toString() }
}

object Prelude {
    fun init(scope: CompilationScope, sb: StringBuilder) {
        scope.addSymbol("println", Type.fn(Type.unit, Type.i32))
        sb.append("""
            void println${'$'}i32(int i) {
              printf("%d\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.i64))
        sb.append("""
            void println${'$'}i64(long i) {
              printf("%d\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.f32))
        sb.append("""
            void println${'$'}f32(float i) {
              printf("%g\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.f64))
        sb.append("""
            void println${'$'}f64(float i) {
              printf("%g\n", i);
            }
        """.trimIndent())
        sb.append('\n')
    }
}

object CEmitter {
    fun emit(tac: List<Tac>): String {
        val sb = StringBuilder()
        tac.forEach {
            // ignored val below is there to force the compiler to check that `when` branches are exhaustive
            val ignored = when(it) {
                is TacAssignment -> sb.append("${it.name} = ${emitOperand(it.value)};\n")
                is TacAssignmentOp -> sb.append("${emitCTypeWithName(it.type, it.name)} = ${emitOperand(it.a)} ${it.op} ${emitOperand(it.b)};\n")
                is TacCall -> {
                    if (it.type != Type.unit) {
                        sb.append(emitCTypeWithName(it.type, it.name))
                        sb.append(" = ")
                    }
                    sb.append("${it.functionName}(")
                    sb.append(it.parameters.joinToString(",") { param -> emitOperand(param) })
                    sb.append(");\n");
                }
                is TacDeclaration -> {
                    sb.append(emitCTypeWithName(it.type, it.name))
                    if (it.value != null) {
                        sb.append(" = ${emitOperand(it.value)}")
                    }
                    sb.append(";\n")
                }
                is TacFunction ->
                    emitFunctionDefinition(it, sb)
                is TacReturn -> sb.append("return ${emitOperand(it.retVal)};\n")
                is TacIfElse -> {
                    sb.append("if(${emitOperand(it.condition)}) {\n")
                    sb.append(emit(it.thenBranch))
                    if (it.elseBranch != null) {
                        sb.append("} else {\n")
                        sb.append(emit(it.elseBranch))
                    }
                    sb.append("};\n")
                }
                is TacNot -> {
                    sb.append("${emitCTypeWithName(it.type, it.name)} = !${emitOperand(it.value)};\n")
                }
                is TacCast -> {
                    sb.append("${emitCTypeWithName(it.type, it.name)} = (${emitCTypeWithName(it.type, null)})${emitOperand(it.value)};\n")
                }
            }
        }

        return sb.toString()
    }

    private fun emitFunctionDefinition(
        tacFunction: TacFunction,
        sb: StringBuilder
    ): java.lang.StringBuilder? {
        val functionNameWithArgs = StringBuilder()
        if (tacFunction.functionName == "main") {
            functionNameWithArgs.append("chi\$main")
        } else {
            functionNameWithArgs.append(tacFunction.functionName)
        }
        functionNameWithArgs.append(' ')
        functionNameWithArgs.append("(")
        if (tacFunction.paramNames.isEmpty()) {
            functionNameWithArgs.append("void")
        } else {
            functionNameWithArgs.append(
                tacFunction.paramsWithTypes.joinToString(", ") { param ->
                    emitCTypeWithName(param.second, param.first)
                }
            )
        }
        functionNameWithArgs.append(")")

        sb.append(emitCTypeWithName(tacFunction.returnType, functionNameWithArgs.toString()))
        sb.append(" {\n")
        sb.append(emit(tacFunction.body))
        return sb.append("}\n")
    }

    private fun emitOperand(operand: Operand): String = when(operand) {
        is TacName -> operand.name
        is TacValue -> operand.value
    }

    private fun emitCTypeWithName(type: Type, name: String?): String {
        return when(type) {
            Type.i32 -> "int ${name?:""}"
            Type.i64 -> "long ${name?:""}"
            Type.f32 -> "float ${name?:""}"
            Type.f64 -> "double ${name?:""}"
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
}
