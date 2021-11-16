package gh.marad.chi.interpreter

import gh.marad.chi.actionast.*
import gh.marad.chi.actionast.Assignment
import gh.marad.chi.actionast.Atom
import gh.marad.chi.actionast.Block
import gh.marad.chi.actionast.Fn
import gh.marad.chi.actionast.FnCall
import gh.marad.chi.actionast.IfElse
import gh.marad.chi.actionast.InfixOp
import gh.marad.chi.actionast.NameDeclaration
import gh.marad.chi.actionast.Program
import gh.marad.chi.actionast.VariableAccess
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

fun tacRepl() {
    val interpreter = TacInterpreter()
    while(true) {
        try {
            print("> ")
            val line = readLine() ?: continue
            if (line.isBlank()) continue
            println(interpreter.eval(line))
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}

private fun printMessages(messages: List<Message>): Boolean {
    messages.forEach { System.err.println(it.message) }
    return messages.isEmpty()
}

private fun show(expr: ActionAst?): String {
    return when(expr) {
        is Atom -> expr.value
        is Fn -> expr.type.name
        null -> ""
        else -> throw RuntimeException("Cannot show expression $expr")
    }
}

object Prelude {
    fun init(interpreter: Interpreter) {
        interpreter.registerNativeFunction("println", Type.fn(Type.unit, Type.i32)) { scope, args ->
            if (args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
            println(show(interpreter.eval(scope, args.first())))
            Atom.unit
        }
    }
}

class ExecutionScope(private val parent: ExecutionScope? = null) {
    private val names = mutableMapOf<String, ActionAst>()

    fun getDefinedNamesAndTypes(): Map<String, Type> =
        names.mapValues { it.value.type }

    fun define(name: String, value: ActionAst) {
        names[name] = value
    }

    fun get(name: String): ActionAst? = names[name] ?: parent?.get(name)
}

sealed interface Value {
    val type: Type
    companion object {
        val unit = UnitValue()
    }
}
data class IntValue(val value: Int) : Value {
    override val type: Type = Type.i32
}

data class BoolValue(val value: Boolean) : Value {
    override val type: Type = Type.bool
}

class UnitValue() : Value {
    override val type: Type = Type.unit
}

data class FunctionParam(val name: String, val type: Type)
data class Function(val params: List<FunctionParam>, val body: List<Tac>, override val type: Type) : Value

class TacScope(private val parent: TacScope? = null) {
    private val names = mutableMapOf<String, Value>()

    fun getNames(): Map<String, Value> = names

    fun getDefinedNamesAndTypes(): Map<String, Type> =
        names.mapValues { it.value.type }

    fun define(name: String, value: Value) { names[name] = value }

    fun get(name: String): Value? = names[name] ?: parent?.get(name)
}

class TacInterpreter {
    val topLevelExecutionScope = TacScope()
    private val nativeFunctions: MutableMap<String, NativeFunction> = mutableMapOf()

    private data class NativeFunction(
        val function: (scope: ExecutionScope, args: List<ActionAst>) -> ActionAst,
        val type: Type,
    )

    fun registerNativeFunction(name: String, type: Type, function: (scope: ExecutionScope, args: List<ActionAst>) -> ActionAst) {
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
        val program = parseProgram(code, getCompilationScope())
        val messages = analyze(program.expressions)
        printMessages(messages)
        return if (messages.isNotEmpty()) {
            null
        } else {
            val tac = TacEmitter().emitProgram(program)
            tac.map { eval(topLevelExecutionScope, it) }.last()
        }
    }

    private fun eval(scope: TacScope, tac: Tac): Value  {
        println("Evaluating $tac...")
        return when(tac) {
            is TacAssignment -> getValue(scope, tac.value, tac.type).let { scope.define(tac.name, it); it }
            is TacAssignmentOp -> evalOp(scope, tac)
            is TacCall -> evalCall(scope, tac)
            is TacDeclaration -> evalDeclaration(scope, tac)
            is TacFunction -> evalFunctionDeclaration(scope, tac)
            is TacIfElse -> evalIfElse(scope, tac)
            is TacReturn -> evalReturn(scope, tac)
        }.also {
            println("result: $it")
        }
    }

    private fun getValue(scope: TacScope, operand: Operand, expectedType: Type): Value = when(operand) {
        is TacName -> scope.get(operand.name)!! // this name should be available since code passed the compiler checks
        is TacValue -> when(expectedType) {
            Type.i32 -> IntValue(operand.value.toInt())
            Type.bool -> BoolValue(operand.value == "true")
            else -> TODO()
        }
    }

    private fun evalOp(scope: TacScope, tac: TacAssignmentOp): Value {
        // FIXME: for some reason this code does not work in repl
        //    val s = fn(): i32 { 5 }
        //    s() + 2
        return when(tac.op) {
            "+" -> {
                val a = getValue(scope, tac.a, tac.type)
                val b = getValue(scope, tac.b, tac.type)
                when(tac.type) {
                    Type.i32 -> IntValue((a as IntValue).value + (b as IntValue).value)
                    else -> TODO()
                }
            }
            else -> TODO("Unsupported infix operation")
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
            val value = getValue(scope, tac.value, tac.type)
            scope.define(tac.name, value)
            value
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

    private fun List<Tac>.eval(scope: TacScope): Value = map { eval(scope, it) }.lastOrNull() ?: Value.unit
}

class Interpreter {
    val topLevelExecutionScope = ExecutionScope()
    private val nativeFunctions: MutableMap<String, NativeFunction> = mutableMapOf()

    private data class NativeFunction(
        val function: (scope: ExecutionScope, args: List<ActionAst>) -> ActionAst,
        val type: Type,
    )

    fun registerNativeFunction(name: String, type: Type, function: (scope: ExecutionScope, args: List<ActionAst>) -> ActionAst) {
        nativeFunctions[name] = NativeFunction(function, type)
    }

    fun getCompilationScope(): CompilationScope {
        val scope = CompilationScope()
        topLevelExecutionScope.getDefinedNamesAndTypes()
            .forEach { scope.defineExternalName(it.key, it.value) }
        nativeFunctions.forEach { scope.defineExternalName(it.key, it.value.type) }
        return scope
    }


    fun eval(code: String): ActionAst? {
        val compilationResult = compile(code, getCompilationScope())
        printMessages(compilationResult.messages)
        return if (compilationResult.hasErrors()) {
            null
        } else {
            compilationResult.program
                .ast
                .map(::eval).last()
        }
    }

    fun eval(expression: ActionAst): ActionAst = eval(topLevelExecutionScope, expression)

    fun eval(scope: ExecutionScope, expression: ActionAst): ActionAst {
        return when (expression) {
            is Program -> TODO()
            is Atom -> expression
            is Assignment -> TODO()
            is VariableAccess -> evalVariableAccess(scope, expression)
            is NameDeclaration -> evalNameDeclaration(scope, expression)
            is Block -> evalBlockExpression(scope, expression)
            is Fn -> expression
            is FnCall -> evalFnCall(scope, expression)
            is IfElse -> evalIfElse(scope, expression)
            is InfixOp -> evalInfixOp(scope, expression)
        }
    }

    private fun evalVariableAccess(scope: ExecutionScope, expr: VariableAccess): ActionAst {
        return scope.get(expr.name) ?: throw RuntimeException("Name ${expr.name} is not recognized")
    }

    private fun evalNameDeclaration(scope: ExecutionScope, expr: NameDeclaration): ActionAst {
        val result = eval(scope, expr.value)
        scope.define(expr.name, result)
        return result
    }

    private fun evalBlockExpression(scope: ExecutionScope, expr: Block): ActionAst {
        return expr.body.map { eval(scope, it) }.lastOrNull() ?: Atom.unit
    }

    private fun evalFnCall(scope: ExecutionScope, expr: FnCall): ActionAst {
        val fnExpr = scope.get(expr.name)
        return if (fnExpr != null && fnExpr is Fn) {
            val subScope = ExecutionScope(scope)
            fnExpr.parameters
                .zip(expr.parameters.map { eval(scope, it) })
                .forEach {
                    subScope.define(it.first.name, it.second)
                }
            val result = eval(subScope, fnExpr.block)
            return if (fnExpr.returnType != Type.unit) {
                result
            } else {
                Atom.unit
            }
        } else if (nativeFunctions.containsKey(expr.name)) {
            val nativeFn = nativeFunctions[expr.name]!!.function
            nativeFn(ExecutionScope(scope), expr.parameters)
        } else {
            throw RuntimeException("There is no function ${expr.name}")
        }
    }

    private fun evalIfElse(scope: ExecutionScope, ifElse: IfElse): ActionAst {
        val result = eval(ifElse.condition)
        return if (result is Atom) {
            if (result == Atom.t) {
                eval(ifElse.thenBranch)
            } else if (ifElse.elseBranch != null) {
                eval(ifElse.elseBranch)
            } else {
                Atom.unit
            }
        } else {
            TODO("if-else condition did not reduce to simple boolean value")
        }
    }

    private fun evalInfixOp(scope: ExecutionScope, op: InfixOp): ActionAst {
        val left = eval(scope, op.left)
        val right = eval(scope, op.right)
        if (left.type == Type.i32 && left is Atom && right is Atom) {
            val lv = left.value.toInt()
            val rv = right.value.toInt()
            val result = when (op.op) {
                "+" -> lv + rv
                "-" -> lv - rv
                "*" -> lv * rv
                "/" -> lv * rv
                else -> TODO("Usupported arithmetic operation exception")
            }
            return Atom.i32(result)
        } else {
            TODO("$left or $right is not an atom")
        }
    }
}