package gh.marad.chi.interpreter

import gh.marad.chi.core.*

fun repl() {
    val scope = Scope()
    Prelude.init(scope)
    while(true) {
        try {
            print("> ")
            val line = readLine() ?: continue
            if (line.isBlank()) continue
            val expressions = parse(tokenize(line))
            val result = expressions.map { scope.eval(it) }.last()
            if (result != Atom("()", Type.unit)) {
                println(show(result))
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
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
    fun init(scope: Scope) {
        scope.addPrintln()
    }

    private fun Scope.addPrintln() = registerNativeFunction("println") { _, args ->
        if(args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
        println(show(args.first()))
        Atom("()", Type.unit)
    }
}

class Scope(names: Map<String, Expression> = mapOf()) {
    val names: MutableMap<String, Expression> = names.toMutableMap()
    private val nativeFunctions: MutableMap<String, (scope: Scope, args: List<Expression>) -> Expression> = mutableMapOf()

    fun registerNativeFunction(name: String, function: (scope: Scope, args: List<Expression>) -> Expression) {
        nativeFunctions[name] = function
    }

    fun copy(): Scope =
        Scope(names)

    fun eval(expression: Expression): Expression {
        return when (expression) {
            is Atom -> expression
            is VariableAccess -> evalVariableAccess(expression)
            is Assignment -> evalAssignment(expression)
            is BlockExpression -> evalBlockExpression(expression)
            is Fn -> expression
            is FnCall -> evalFnCall(expression)
        }
    }

    private fun evalVariableAccess(expr: VariableAccess): Expression {
        return names[expr.name]
            ?: throw RuntimeException("Name '${expr.name}' is not recognized")
    }

    private fun evalAssignment(expr: Assignment): Expression {
        // TODO: brak wykorzystania mutable/immutable
        // pewnie names powinno mieć jakiś dodatkowy typ Variable, który by o tym decydował
        // być może to jest spoko bo to się powinno wywalić na etapie analizy
        val result = eval(expr.value)
        names[expr.name] = result
        return result
    }

    private fun evalBlockExpression(expr: BlockExpression): Expression {
        return expr.body.map { eval(it) }.lastOrNull() ?: Atom("()", Type.unit)
    }

    private fun evalFnCall(expr: FnCall): Expression {
        return when {
            names.containsKey(expr.name) -> {
                val fn = names[expr.name] as Fn
                val subscope = copy()
                fn.parameters
                    .zip(expr.parameters.map { eval(it) })
                    .forEach {
                        subscope.names[it.first.name] = it.second
                    }
                subscope.eval(fn.body)
            }
            nativeFunctions.containsKey(expr.name) -> {
                val nativeFn = nativeFunctions[expr.name]!!
                nativeFn(copy(), expr.parameters)
            }
            else -> TODO()
        }
    }
}