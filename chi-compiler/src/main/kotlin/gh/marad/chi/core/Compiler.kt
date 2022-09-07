@file:Suppress("BooleanMethodIsAlwaysInverted")

package gh.marad.chi.core


data class CompilationResult(
    val messages: List<Message>,
    val program: Program,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
    fun errors() = messages.filter { it.level == Level.ERROR }
}

object Compiler {
    /**
     * Compiles source code and produces compilation result that
     * contains AST and compilation messages.
     *
     * @param source Chi source code.
     * @param namespace Namespace to use for compilation
     */
    @JvmStatic
    fun compile(source: String, namespace: GlobalCompilationNamespace): CompilationResult {
        val (program, parsingMessages) = parseProgram(source, namespace)
        return if (parsingMessages.isNotEmpty()) {
            CompilationResult(parsingMessages, program)
        } else {
            val messages = analyze(program)
            CompilationResult(messages, program)
        }
    }

    @JvmStatic
    fun formatCompilationMessage(source: String, message: Message): String {
        val location = message.location
        val sb = StringBuilder()
        if (location != null) {
            val sourceLine = source.lines()[location.start.line - 1]
            sb.appendLine(sourceLine)
            repeat(location.start.column) { sb.append(' ') }
            sb.append("^ ")
        }
        sb.append(message.message)
        return sb.toString()
    }
}
