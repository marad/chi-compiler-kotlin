package gh.marad.chi

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.Compiler
import gh.marad.chi.core.Expression
import gh.marad.chi.core.analyzer.Message
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace

data class ErrorMessagesException(val errors: List<Message>) : AssertionError("Chi compilation errors")

fun compile(
    code: String,
    namespace: GlobalCompilationNamespace = GlobalCompilationNamespace(),
    ignoreCompilationErrors: Boolean = false
): List<Expression> {
    val result = Compiler.compile(code, namespace)

    if (!ignoreCompilationErrors) {
        result.messages.forEach { msg ->
            System.err.println(Compiler.formatCompilationMessage(code, msg))
            System.err.flush()
        }

        if (result.hasErrors()) {
            throw ErrorMessagesException(result.errors())
        }
    }

    return result.program.expressions
}

fun asts(
    code: String,
    scope: CompilationScope = CompilationScope(),
    ignoreCompilationErrors: Boolean = false
): List<Expression> {
    val namespace = GlobalCompilationNamespace()
    namespace.setPackageScope(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge, scope)
    return compile(code, namespace, ignoreCompilationErrors)
}

fun ast(
    code: String,
    scope: CompilationScope = CompilationScope(),
    ignoreCompilationErrors: Boolean = false
): Expression = asts(code, scope, ignoreCompilationErrors).last()
