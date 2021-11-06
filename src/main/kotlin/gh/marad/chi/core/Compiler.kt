package gh.marad.chi.core

import gh.marad.chi.core.analyzer.Scope

data class CompilationResult(
    val messages: List<Message>,
    val ast: List<Expression>,
    val scope: Scope<Expression>,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
}

/**
 * Compiles source code and produces compilation result that
 * contains AST and compilation messages.
 *
 * @param source Chi source code.
 * @param parentScope Optional scope, so you can add external names.
*/
fun compile(source: String, parentScope: Scope<Expression>? = null): CompilationResult {
    val ast = parse(tokenize(source))
    val scope = Scope.fromExpressions(ast, parentScope)
    val messages = analyze(scope, ast)
    return CompilationResult(messages, ast, scope)
}