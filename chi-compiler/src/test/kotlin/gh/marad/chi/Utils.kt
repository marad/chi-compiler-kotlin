package gh.marad.chi

import gh.marad.chi.core.CompilationDefaults
import gh.marad.chi.core.Compiler
import gh.marad.chi.core.Expression
import gh.marad.chi.core.analyzer.Message
import gh.marad.chi.core.compiled.Compiled
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.parseProgram

data class ErrorMessagesException(val errors: List<Message>) : AssertionError("Chi compilation errors")

fun expressions(
    code: String,
    namespace: GlobalCompilationNamespace = GlobalCompilationNamespace(),
): List<Expression> {
    val (program, parserMessages) = parseProgram(code, namespace)
//    if (parserMessages.isNotEmpty()) {
//        throw ErrorMessagesException(parserMessages)
//    }
    return program.expressions
}

fun expr(
    code: String,
    scope: CompilationScope = CompilationScope(ScopeType.Package),
): Expression {
    val namespace = GlobalCompilationNamespace()
    namespace.setPackageScope(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge, scope)
    return expressions(code, namespace).last()
}

fun compile(
    code: String,
    namespace: GlobalCompilationNamespace = GlobalCompilationNamespace(),
    ignoreCompilationErrors: Boolean = false
): List<Compiled> {
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

    return result.code.body
}

fun asts(
    code: String,
    scope: CompilationScope = CompilationScope(ScopeType.Package),
    ignoreCompilationErrors: Boolean = false
): List<Compiled> {
    val namespace = GlobalCompilationNamespace()
    namespace.setPackageScope(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge, scope)
    return compile(code, namespace, ignoreCompilationErrors)
}

fun ast(
    code: String,
    scope: CompilationScope = CompilationScope(ScopeType.Package),
    ignoreCompilationErrors: Boolean = false
): Compiled = asts(code, scope, ignoreCompilationErrors).last()
