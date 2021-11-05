package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.Scope

fun repl() {
    val interpreter = Interpreter()
    val scope = Scope()
    Prelude.init(scope, interpreter)
    while(true) {
        try {
            print("> ")
            val line = readLine() ?: continue
            if (line.isBlank()) continue
            val compilationResult = compile(line, scope)
            printMessages(compilationResult.messages)
            if (!compilationResult.hasErrors()) {
                val result = compilationResult.ast.map {
                    interpreter.eval(compilationResult.scope, it)
                }.last()
                println(show(result))
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}

private fun printMessages(messages: List<Message>): Boolean {
    messages.forEach { System.err.println(it.message) }
    return messages.isEmpty()
}

private fun show(expr: Expression): String {
    return when(expr) {
        is Atom -> expr.value
        is Fn -> expr.type.name
        else -> throw RuntimeException("Cannot show expression $expr")
    }
}

object Prelude {
    fun init(scope: Scope, interpreter: Interpreter) {
        scope.defineExternalName("println", Type.fn(Type.unit, Type.i32))
        interpreter.registerNativeFunction("println") { _, args ->
            if (args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
            println(show(interpreter.eval(scope, args.first())))
            Atom.unit(null)
        }
    }
}

class Interpreter {
    private val nativeFunctions: MutableMap<String, (scope: Scope, args: List<Expression>) -> Expression> = mutableMapOf()

    fun registerNativeFunction(name: String, function: (scope: Scope, args: List<Expression>) -> Expression) {
        nativeFunctions[name] = function
    }

    fun eval(scope: Scope, expression: Expression): Expression {
        return when (expression) {
            is Atom -> expression
            is Assignment -> TODO()
            is VariableAccess -> evalVariableAccess(scope, expression)
            is NameDeclaration -> evalNameDeclaration(scope, expression)
            is BlockExpression -> evalBlockExpression(scope, expression)
            is Fn -> expression
            is FnCall -> evalFnCall(scope, expression)
            is IfElse -> TODO()
        }
    }

    private fun evalVariableAccess(scope: Scope, expr: VariableAccess): Expression {
        return scope.findVariable(expr.name) ?: throw RuntimeException("Name ${expr.name} is not recognized")
    }

    private fun evalNameDeclaration(scope: Scope, expr: NameDeclaration): Expression {
        val result = eval(scope, expr.value)
        scope.defineVariable(expr.name, result)
        return result
    }

    private fun evalBlockExpression(scope: Scope, expr: BlockExpression): Expression {
        return expr.body.map { eval(scope, it) }.lastOrNull() ?: Atom.unit(expr.location)
    }

    private fun evalFnCall(scope: Scope, expr: FnCall): Expression {
        val fnExpr = scope.findVariable(expr.name)
        if (fnExpr != null && fnExpr is Fn) {
            val fn = fnExpr
            val subscope = Scope(scope)
            fn.parameters.forEach { subscope.defineExternalName(it.name, it.type) }

            fn.parameters
                .zip(expr.parameters.map { eval(subscope, it) })
                .forEach {
                    subscope.defineVariable(it.first.name, it.second)
                }
            return eval(subscope, fn.block)
        } else if (nativeFunctions.containsKey(expr.name)) {
            val nativeFn = nativeFunctions[expr.name]!!
            return nativeFn(Scope(scope), expr.parameters)
        } else {
            throw RuntimeException("There is no function ${expr.name}")
        }
    }
}