@file:Suppress("BooleanMethodIsAlwaysInverted")

package gh.marad.chi.core

import gh.marad.chi.core.analyzer.Level
import gh.marad.chi.core.analyzer.Message
import gh.marad.chi.core.analyzer.analyze
import gh.marad.chi.core.namespace.GlobalCompilationNamespace


data class CompilationResult(
    val messages: List<Message>,
    val program: Program,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
    fun errors() = messages.filter { it.level == Level.ERROR }
    fun validate(code: String): Boolean {
        return hasErrors().also {
            errors().forEach {
                System.err.println(Compiler.formatCompilationMessage(code, it))
            }
        }
    }
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
        val sourceSection = message.codePoint
        val sb = StringBuilder()
        if (sourceSection != null) {
            val sourceLine = source.lines()[sourceSection.line - 1]
            sb.appendLine(sourceLine)
            repeat(sourceSection.column) { sb.append(' ') }
            sb.append("^ ")
        }
        sb.append(message.message)
        return sb.toString()
    }
}
