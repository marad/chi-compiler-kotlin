package gh.marad.chi.core

import gh.marad.chi.actionast.ActionAst

data class CompilationResult(
    val messages: List<Message>,
    val ast: List<ActionAst>,
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
fun compile(source: String, parentScope: CompilationScope? = null): CompilationResult {
    val ast = parse(tokenize(source), parentScope)
    val messages = analyze(ast)
    return CompilationResult(messages, ActionAst.from(ast))
}