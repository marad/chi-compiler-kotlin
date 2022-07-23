package gh.marad.chi

import gh.marad.chi.core.*

fun asts(code: String, scope: CompilationScope = CompilationScope(), ignoreCompilationErrors: Boolean = false): List<Expression> {
    val result = Compiler.compile(code, scope)

    if (!ignoreCompilationErrors) {
        result.messages.forEach { msg ->
            System.err.println(Compiler.formatCompilationMessage(code, msg))
            System.err.flush()
        }

        if (result.hasErrors()) {
            throw AssertionError("Chi compilation errors!")
        }
    }

    return result.program.expressions
}
fun ast(code: String, scope: CompilationScope = CompilationScope(), ignoreCompilationErrors: Boolean = false): Expression = asts(code, scope, ignoreCompilationErrors).last()

