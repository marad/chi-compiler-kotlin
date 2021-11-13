package gh.marad.chi.core

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

//class UnexpectedToken(val token: Token, val expected: String? = null, val suggestion: String? = null) :
//        RuntimeException("Unexpected token '${token.value}' at line ${token.location.formattedPosition}.${if (expected != null) " Expected to find '$expected'." else ""}${if (suggestion != null) " Suggestion: $suggestion." else ""}")
//
//class OneOfTokensExpected(val expected: List<String>, val actual: Token) :
//        RuntimeException("Expected one of ('${expected.joinToString("', '")}') but got '${actual.value}' at line ${actual.location.formattedPosition}")
//

class ThrowingErrorListener : BaseErrorListener() {
        override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String?,
                e: RecognitionException?
        ) {
                throw Exception()
        }

}