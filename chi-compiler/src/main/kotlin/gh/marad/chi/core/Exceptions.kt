package gh.marad.chi.core

import ChiLexer
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
        val location = Location(
            LocationPoint(line, charPositionInLine),
            end = LocationPoint(line, charPositionInLine),
            0,
            0
        )


        if (e?.inputStream?.firstToken() == ChiLexer.IMPORT) {
            messages.add(InvalidImport(msg, location))
        } else {
            messages.add(SyntaxError(offendingSymbol, msg, location))
        }
    }

    private fun IntStream.firstToken(): Int = LA(-index())
}