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
    val cCode = CEmitter.emit(compilationScope.getComplexTypes(), compilationResult.program)
    return TranspileResult(cCode, compilationResult.messages)
}

fun main() {
    val type = ComplexType("Person", listOf(
        ComplexTypeField("age", Type.i32),
        ComplexTypeField("salary", Type.f32)
    ))

    val sb = StringBuilder()
    CEmitter.emitComplexTypeDefinitions(listOf(type), sb)
    println(sb.toString())
}

object CEmitter {

    fun emit(complexTypes: List<ComplexType>, tac: List<Tac>): String {
        val sb = StringBuilder()
        emitComplexTypeDefinitions(complexTypes, sb)
        emitCode(tac, sb)
        return sb.toString()
    }

    fun emitComplexTypeDefinitions(types: List<ComplexType>, sb: StringBuilder) {
        types.forEach {
            emitComplexTypeDefinition(it, sb)
            emitComplexTypeConstructor(it, sb)
        }
    }

    private fun emitComplexTypeDefinition(type: ComplexType, sb: StringBuilder) {
        sb.appendLine("struct ${type.name} {")
        type.fields.forEach {
            sb.append(emitCTypeWithName(it.type, it.name))
            sb.appendLine(";")
        }
        sb.appendLine("};")
    }

    private fun emitComplexTypeConstructor(type: ComplexType, sb: StringBuilder) {
        val functionName = makeFunctionName(type.name, type.fields.map { it.type })
        emitFunctionHeader(functionName, type.fields.map { it.name to it.type }, type, sb)
        sb.appendLine(" {")
        sb.append(emitCTypeWithName(type, "data"))
        sb.append(" = (")
        sb.append(emitCTypeWithName(type, null))
        sb.appendLine(") GC_malloc(sizeof(struct ${type.name}));")
        type.fields.forEach {
            sb.appendLine("data->${it.name} = ${it.name};")
        }
        sb.appendLine("return data;")
        sb.appendLine("}")
    }

    fun emitCode(tac: List<Tac>, sb: StringBuilder) {
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
                    sb.append(emitCode(it.thenBranch, sb))
                    if (it.elseBranch != null) {
                        sb.append("} else {\n")
                        sb.append(emitCode(it.elseBranch, sb))
                    }
                    sb.append("};\n")
                }
                is TacNot -> {
                    sb.append("${emitCTypeWithName(it.type, it.name)} = !${emitOperand(it.value)};\n")
                }
                is TacCast -> {
                    sb.append("${emitCTypeWithName(it.type, it.name)} = (${emitCTypeWithName(it.type, null)})${emitOperand(it.value)};\n")
                }
                is TacFieldAccess -> {
                    sb.append("${emitCTypeWithName(it.type, it.name)} = ${it.subject.name}->${it.field};\n")
                }
            }
        }
    }

    private fun emitFunctionDefinition(
        tacFunction: TacFunction,
        sb: StringBuilder
    ): java.lang.StringBuilder? {
        emitFunctionHeader(
            tacFunction.functionName,
            tacFunction.paramsWithTypes,
            tacFunction.returnType,
            sb
        )
        sb.append(" {\n")
        emitCode(tacFunction.body, sb)
        return sb.append("}\n")
    }

    private fun emitFunctionHeader(functionName: String,
                                   params: List<Pair<String, Type>>,
                                   returnType: Type,
                                   sb: StringBuilder) {
        val functionNameWithArgs = StringBuilder()
        if (functionName == "main") {
            functionNameWithArgs.append("chi_main")
        } else {
            functionNameWithArgs.append(functionName)
        }
        functionNameWithArgs.append(' ')
        functionNameWithArgs.append("(")
        if (params.isEmpty()) {
            functionNameWithArgs.append("void")
        } else {
            functionNameWithArgs.append(
                params.joinToString(", ") { param ->
                    emitCTypeWithName(param.second, param.first)
                }
            )
        }
        functionNameWithArgs.append(")")

        sb.append(emitCTypeWithName(returnType, functionNameWithArgs.toString()))
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
            is ComplexType -> {
                "struct ${type.name}* ${name?:""}"
            }
            else -> throw RuntimeException("Unsupported type ${type}!")
        }
    }
}
