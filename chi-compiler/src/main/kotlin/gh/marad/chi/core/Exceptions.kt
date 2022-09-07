package gh.marad.chi.core

import ChiLexer
import gh.marad.chi.core.analyzer.CodePoint
import gh.marad.chi.core.analyzer.InvalidImport
import gh.marad.chi.core.analyzer.Message
import gh.marad.chi.core.analyzer.SyntaxError
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class MessageCollectingErrorListener : BaseErrorListener() {
    private val messages = mutableListOf<Message>()

    fun getMessages(): List<Message> = messages

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        val point = CodePoint(line, charPositionInLine)

        if (e?.inputStream?.firstToken() == ChiLexer.IMPORT) {
            messages.add(InvalidImport(msg, point))
        } else {
            messages.add(SyntaxError(offendingSymbol, msg, point))
        }
    }

    private fun IntStream.firstToken(): Int = LA(-index())
}