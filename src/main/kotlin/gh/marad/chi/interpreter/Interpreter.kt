package gh.marad.chi.interpreter

import gh.marad.chi.actionast.*
import gh.marad.chi.core.Message
import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.Type
import gh.marad.chi.core.compile

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
            is IfElse -> TODO()
            is InfixOp -> TODO()
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
}