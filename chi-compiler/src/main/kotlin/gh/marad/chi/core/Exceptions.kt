package gh.marad.chi.core

import org.antlr.v4.runtime.BaseErrorListener
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
                messages.add(SyntaxError(offendingSymbol, Location(LocationPoint(line, charPositionInLine), end = LocationPoint(line, charPositionInLine), 0, 0), msg))
        }
}