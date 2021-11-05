package gh.marad.chi.core

import gh.marad.chi.core.analyzer.Scope

data class CompilationResult(
    val messages: List<Message>,
    val ast: List<Expression>,
    val scope: Scope,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
}

fun compile(source: String, parentScope: Scope? = null): CompilationResult {
    val ast = parse(tokenize(source))
    val scope = Scope.fromExpressions(ast, parentScope)
    val messages = analyze(scope, ast)
    return CompilationResult(messages, ast, scope)
}