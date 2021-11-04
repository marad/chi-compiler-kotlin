package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.core.analyze
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
            val expressions = parse(tokenize(line))
            if (staticChecksOk(scope, expressions)) {
                val result = expressions.map { interpreter.eval(scope, it) }.last()
                if (result is Atom && result.type != Type.unit) {
                    println(show(result))
                }
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}

private fun staticChecksOk(scope: Scope, exprs: List<Expression>): Boolean {
    val messages = analyze(scope, exprs)
    messages.forEach { System.err.println(it.message) }
    return messages.isEmpty()
}

private fun show(expr: Expression): String {
    return when(expr) {
        is Atom -> expr.value
        is Fn -> {
            val paramTypes = expr.parameters.map { it.type }.joinToString(", ")
            val returnType = expr.returnType
            "fn($paramTypes) -> $returnType"
        }
        else -> throw RuntimeException("Cannot show expression $expr")
    }
}

object Prelude {
    fun init(scope: Scope, interpreter: Interpreter) {
        scope.defineExternalName("println", Type.unit)
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
            is VariableAccess -> evalVariableAccess(scope, expression)
            is Assignment -> evalAssignment(scope, expression)
            is BlockExpression -> evalBlockExpression(scope, expression)
            is Fn -> expression
            is FnCall -> evalFnCall(scope, expression)
        }
    }

    private fun evalVariableAccess(scope: Scope, expr: VariableAccess): Expression {
        return scope.findVariable(expr.name) ?: throw RuntimeException("Name ${expr.name} is not recognized")
    }

    private fun evalAssignment(scope: Scope, expr: Assignment): Expression {
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
            val fn = fnExpr as Fn
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