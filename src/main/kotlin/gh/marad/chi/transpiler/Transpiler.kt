package gh.marad.chi.transpiler

import gh.marad.chi.core.*
import gh.marad.chi.tac.*

fun <T> time(comment: String, f: () -> T): T {
    val start = System.nanoTime()
    val result = f()
    val end = System.nanoTime()
    val duration = (end-start) / 1000_000
    println("$comment - $duration ms")
    return result
}

data class TranspileResult(
    val cCode: String,
    val messages: List<Message>
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
    fun hasMessages() = messages.isNotEmpty()
}

fun transpile(code: String, compilationScope: CompilationScope): TranspileResult {
    val compilationResult = compile(code, compilationScope)
    val cCode = CEmitter.emit(compilationResult.program)
    return TranspileResult(cCode, compilationResult.messages)
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
