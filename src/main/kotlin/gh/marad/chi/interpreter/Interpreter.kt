package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.tac.*

fun repl() {
    val interpreter = Interpreter()
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

internal fun show(v: Value?): String = v?.show() ?: ""

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
    fun show(): String
    fun cast(targetType: Type): Value
    companion object {
        val unit = UnitValue()
        fun i32(value: Int) = IntValue(value)
        fun bool(value: Boolean) = BoolValue(value)
    }
}
data class IntValue(val value: Int) : Value {
    override val type: Type = Type.i32
    override fun show(): String = value.toString()
    override fun cast(targetType: Type) = when(targetType) {
        Type.i32 -> this
        Type.i64 -> LongValue(value.toLong())
        Type.f32 -> FloatValue(value.toFloat())
        Type.f64 -> DoubleValue(value.toDouble())
        else -> TODO("Some good exception")
    }
}

data class LongValue(val value: Long) : Value {
    override val type: Type = Type.i64
    override fun show(): String = value.toString()
    override fun cast(targetType: Type) = when(targetType) {
        Type.i32 -> IntValue(value.toInt())
        Type.i64 -> this
        Type.f32 -> FloatValue(value.toFloat())
        Type.f64 -> DoubleValue(value.toDouble())
        else -> TODO("Some good exception")
    }
}

data class FloatValue(val value: Float) : Value {
    override val type: Type = Type.f32
    override fun show(): String = value.toString()
    override fun cast(targetType: Type) = when(targetType) {
        Type.i32 -> IntValue(value.toInt())
        Type.i64 -> LongValue(value.toLong())
        Type.f32 -> this
        Type.f64 -> DoubleValue(value.toDouble())
        else -> TODO("Some good exception")
    }
}

data class DoubleValue(val value: Double) : Value {
    override val type: Type = Type.f64
    override fun show(): String = value.toString()
    override fun cast(targetType: Type) = when(targetType) {
        Type.i32 -> IntValue(value.toInt())
        Type.i64 -> LongValue(value.toLong())
        Type.f32 -> FloatValue(value.toFloat())
        Type.f64 -> this
        else -> TODO("Some good exception")
    }
}

data class BoolValue(val value: Boolean) : Value {
    override val type: Type = Type.bool
    override fun show(): String = value.toString()
    override fun cast(targetType: Type): Value = TODO("Why would you do that?")
}

class UnitValue : Value {
    override val type: Type = Type.unit
    override fun show(): String = "()"
    override fun cast(targetType: Type): Value = TODO("Just no")
}

data class FunctionParam(val name: String, val type: Type)
data class Function(val params: List<FunctionParam>, val body: List<Tac>, override val type: Type) : Value {
    override fun show(): String = type.toString()
    override fun cast(targetType: Type): Value = TODO("Oh, hell no!")
}

data class NativeFunctionValue(val fn: (scope: TacScope, args: List<Value>) -> Value, override val type: FnType) : Value {
    override fun show(): String = type.toString()
    override fun cast(targetType: Type): Value = TODO("Nope, nope, nope!")
}

class TacScope(private val parent: TacScope? = null) {
    private val names = mutableMapOf<String, Value>()

    fun getDefinedNamesAndTypes(): Map<String, Type> =
        names.mapValues { it.value.type }

    fun define(name: String, value: Value) { names[name] = value }

    fun get(name: String): Value? = names[name] ?: parent?.get(name)
}

class Interpreter(private val debug: Boolean = false) {
    val topLevelExecutionScope = TacScope()

    init {
        Prelude.init(this)
    }

    private data class NativeFunction(
        val function: (scope: TacScope, args: List<Value>) -> Value,
        val type: Type,
    )

    fun registerNativeFunction(name: String, type: FnType, function: (scope: TacScope, args: List<Value>) -> Value) {
        topLevelExecutionScope.define(name, NativeFunctionValue(function, type))
    }

    private fun getCompilationScope(): CompilationScope {
        val scope = CompilationScope()
        topLevelExecutionScope.getDefinedNamesAndTypes()
            .forEach { scope.defineExternalName(it.key, it.value) }
        return scope
    }

    fun eval(code: String): Value? {
        val result = compile(code, getCompilationScope())
        printMessages(result.messages)
        return if (result.messages.isNotEmpty()) {
            null
        } else {
            result.program.map {
                if (debug) println("Evaluating $it...")
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
            is TacCast -> evalCast(scope, tac)
        }.also {
            scope.define(tac.name, it)
        }
    }

    fun getValue(scope: TacScope, operand: Operand, expectedType: Type?): Value = when(operand) {
        is TacName -> scope.get(operand.name)!! // this name should be available since code passed the compiler checks
        is TacValue -> when(expectedType) {
            Type.i32 -> IntValue(operand.value.toInt())
            Type.i64 -> LongValue(operand.value.toLong())
            Type.f32 -> FloatValue(operand.value.toFloat())
            Type.f64 -> DoubleValue(operand.value.toDouble())
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
            Type.i64 -> {
                val a = getValue(scope, tac.a, tac.type) as LongValue
                val b = getValue(scope, tac.b, tac.type) as LongValue
                when(tac.op) {
                    "+" -> LongValue(a.value+b.value)
                    "-" -> LongValue(a.value-b.value)
                    "*" -> LongValue(a.value*b.value)
                    "/" -> LongValue(a.value/b.value)
                    "%" -> LongValue(a.value%b.value)
                    else -> TODO("Unsupported infix operation")
                }
            }
            Type.f32 -> {
                val a = getValue(scope, tac.a, tac.type) as FloatValue
                val b = getValue(scope, tac.b, tac.type) as FloatValue
                when(tac.op) {
                    "+" -> FloatValue(a.value+b.value)
                    "-" -> FloatValue(a.value-b.value)
                    "*" -> FloatValue(a.value*b.value)
                    "/" -> FloatValue(a.value/b.value)
                    "%" -> FloatValue(a.value%b.value)
                    else -> TODO("Unsupported infix operation")
                }
            }
            Type.f64 -> {
                val a = getValue(scope, tac.a, tac.type) as DoubleValue
                val b = getValue(scope, tac.b, tac.type) as DoubleValue
                when(tac.op) {
                    "+" -> DoubleValue(a.value+b.value)
                    "-" -> DoubleValue(a.value-b.value)
                    "*" -> DoubleValue(a.value*b.value)
                    "/" -> DoubleValue(a.value/b.value)
                    "%" -> DoubleValue(a.value%b.value)
                    else -> TODO("Unsupported infix operation")
                }
            }
            Type.bool -> {
                val a = getValue(scope, tac.a, tac.type) as BoolValue
                val b = getValue(scope, tac.b, tac.type) as BoolValue
                when(tac.op) {
                    "&&" -> BoolValue(a.value && b.value)
                    "||" -> BoolValue(a.value || b.value)
                    else -> TODO("Unsupported infix operation")
                }
            }
            else -> TODO()
        }
    }

    private fun evalCall(scope: TacScope, tac: TacCall): Value {
        val subscope = TacScope(scope)
        return when(val func = scope.get(tac.functionName)) {
            is Function -> {
                func.params
                    .zip(tac.parameters)
                    .forEach {
                        subscope.define(it.first.name, getValue(subscope, it.second, it.first.type))
                    }
                func.body.eval(subscope)
            }
            is NativeFunctionValue -> {
                val params = func.type.paramTypes
                    .zip(tac.parameters)
                    .map {
                        getValue(subscope, it.second, it.first)
                    }
                func.fn(subscope, params)
            }
            else -> TODO("This is not a function!")
        }
    }

    private fun evalDeclaration(scope: TacScope, tac: TacDeclaration): Value =
        if (tac.value != null) {
            getValue(scope, tac.value, tac.type)
        } else {
            Value.unit
        }

    private fun evalCast(scope: TacScope, tac: TacCast): Value {
        val value = getValue(scope, tac.value, null)
        return value.cast(tac.type)
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