package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.tac.*

fun repl() {
    val interpreter = Interpreter()
    Prelude.init(interpreter)
    while(true) {
        try {
            print("> ")
            val line = readLine() ?: continue
            if (line.isBlank()) continue
            println(show(interpreter.eval(line)))
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}

private fun printMessages(messages: List<Message>): Boolean {
    messages.forEach { System.err.println(it.message) }
    return messages.isEmpty()
}

private fun show(v: Value?): String {
    return when(v) {
        is IntValue -> v.value.toString()
        is BoolValue -> v.value.toString()
        is Function -> v.type.toString()
        is UnitValue -> ""
        null -> ""
    }
}

object Prelude {
    fun init(interpreter: Interpreter) {
        interpreter.registerNativeFunction("println", Type.fn(Type.unit, Type.i32)) { _, args ->
            if (args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
            println(show(args.first()))
            Value.unit
        }
    }
}

sealed interface Value {
    val type: Type
    companion object {
        val unit = UnitValue()
        fun i32(value: Int) = IntValue(value)
        fun bool(value: Boolean) = BoolValue(value)
    }
}
data class IntValue(val value: Int) : Value {
    override val type: Type = Type.i32
}

data class BoolValue(val value: Boolean) : Value {
    override val type: Type = Type.bool
}

class UnitValue : Value {
    override val type: Type = Type.unit
}

data class FunctionParam(val name: String, val type: Type)
data class Function(val params: List<FunctionParam>, val body: List<Tac>, override val type: Type) : Value

class TacScope(private val parent: TacScope? = null) {
    private val names = mutableMapOf<String, Value>()

    fun getDefinedNamesAndTypes(): Map<String, Type> =
        names.mapValues { it.value.type }

    fun define(name: String, value: Value) { names[name] = value }

    fun get(name: String): Value? = names[name] ?: parent?.get(name)
}

class Interpreter(private val debug: Boolean = false) {
    val topLevelExecutionScope = TacScope()
    private val nativeFunctions: MutableMap<String, NativeFunction> = mutableMapOf()

    private data class NativeFunction(
        val function: (scope: TacScope, args: List<Value>) -> Value,
        val type: Type,
    )

    fun registerNativeFunction(name: String, type: Type, function: (scope: TacScope, args: List<Value>) -> Value) {
        nativeFunctions[name] = NativeFunction(function, type)
    }

    private fun getCompilationScope(): CompilationScope {
        val scope = CompilationScope()
        topLevelExecutionScope.getDefinedNamesAndTypes()
            .forEach { scope.defineExternalName(it.key, it.value) }
        nativeFunctions.forEach { scope.defineExternalName(it.key, it.value.type) }
        return scope
    }

    fun eval(code: String): Value? {
        val (program, parsingMessages) = parseProgram(code, getCompilationScope())
        val analysisMessages = analyze(program.expressions)
        val messages = parsingMessages + analysisMessages
        printMessages(messages)
        return if (messages.isNotEmpty()) {
            null
        } else {
            val tac = TacEmitter().emitProgram(program)
            tac.map {
                if (debug) println("Evaluating $tac...")
                EvalModule.eval(topLevelExecutionScope, it).also {
                    if (debug) println("result: $it")
                }
            }.last()
        }
    }
}

object EvalModule {
    fun eval(scope: TacScope, tac: Tac): Value  {
        return when(tac) {
            is TacAssignment -> getValue(scope, tac.value, tac.type)
            is TacAssignmentOp -> evalOp(scope, tac)
            is TacCall -> evalCall(scope, tac)
            is TacDeclaration -> evalDeclaration(scope, tac)
            is TacFunction -> evalFunctionDeclaration(scope, tac)
            is TacIfElse -> evalIfElse(scope, tac)
            is TacReturn -> evalReturn(scope, tac)
            is TacNot -> evalNot(scope, tac)
        }.also {
            scope.define(tac.name, it)
        }
    }

    fun getValue(scope: TacScope, operand: Operand, expectedType: Type): Value = when(operand) {
        is TacName -> scope.get(operand.name)!! // this name should be available since code passed the compiler checks
        is TacValue -> when(expectedType) {
            Type.i32 -> IntValue(operand.value.toInt())
            Type.bool -> BoolValue(operand.value == "true")
            else -> TODO()
        }
    }

    private fun evalOp(scope: TacScope, tac: TacAssignmentOp): Value {
        return when(tac.type) {
            Type.i32 -> {
                val a = getValue(scope, tac.a, tac.type) as IntValue
                val b = getValue(scope, tac.b, tac.type) as IntValue
                when(tac.op) {
                    "+" -> IntValue(a.value+b.value)
                    "-" -> IntValue(a.value-b.value)
                    "*" -> IntValue(a.value*b.value)
                    "/" -> IntValue(a.value/b.value)
                    "%" -> IntValue(a.value%b.value)
                    else -> TODO("Unsupported infix operation")
                }
            }
            else -> TODO()
        }
    }

    private fun evalCall(scope: TacScope, tac: TacCall): Value {
        val func = scope.get(tac.functionName) as Function
        val subscope = TacScope(scope)
        func.params
            .zip(tac.parameters)
            .forEach {
                subscope.define(it.first.name, getValue(subscope, it.second, it.first.type))
            }
        return func.body.eval(subscope)
    }

    private fun evalDeclaration(scope: TacScope, tac: TacDeclaration): Value =
        if (tac.value != null) {
            getValue(scope, tac.value, tac.type)
        } else {
            Value.unit
        }

    private fun evalFunctionDeclaration(scope: TacScope, tac: TacFunction): Value {
        val params = tac.paramsWithTypes.map { FunctionParam(it.first, it.second) }
        val result = Function(params, tac.body, tac.type)
        scope.define(tac.functionName, result)
        return result
    }

    private fun evalIfElse(scope: TacScope, tac: TacIfElse): Value {
        val conditionResult = getValue(scope, tac.condition, Type.bool) as BoolValue
        return if (conditionResult.value) {
            tac.thenBranch.eval(scope)
        } else {
            tac.elseBranch?.eval(scope) ?: Value.unit
        }
    }

    private fun evalReturn(scope: TacScope, tac: TacReturn): Value {
        return getValue(scope, tac.retVal, tac.type)
    }

    private fun evalNot(scope: TacScope, tac: TacNot): Value {
        val value = getValue(scope, tac.value, tac.type) as BoolValue
        return BoolValue(!value.value)
    }


    private fun List<Tac>.eval(scope: TacScope): Value = map { eval(scope, it) }.lastOrNull() ?: Value.unit
}