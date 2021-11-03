package gh.marad.chi.core

enum class TokenType {
    KEYWORD, SYMBOL, OPERATOR, INTEGER
}

data class Token(val type: TokenType, val value: String)

fun tokenize(source: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val tokenizer = Tokenizer(source.toCharArray())

    while(!tokenizer.isEof()) {
        val char = tokenizer.peekChar()
        when {
            char == null -> throw RuntimeException("Read `null` character!")
            char.isWhitespace() -> tokenizer.skipWhitespace()
            char.isLetter() -> tokens.add(tokenizer.readSymbolOrKeyword())
            char.isDigit() -> tokens.add(tokenizer.readNumber())
            char in operatorChars -> tokens.add(tokenizer.readOperator())
            else -> throw RuntimeException("Unhandled character: $char")
        }
    }

    return tokens
}

private val keywords = arrayListOf("val", "var", "fn", "i32")
private val numberChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
private val operatorChars = charArrayOf('=', '+', '-', '/', '*', '{', '}', '(', ')', ':', ',')


private class Tokenizer(private var source: CharArray) {
    private var currentPosition = 0
    fun isEof(): Boolean = currentPosition >= source.size
    fun skipWhitespace() {
        while(true) {
            val char = peekChar()
            if (char != null && char.isWhitespace()) {
                currentPosition++
            } else {
                break
            }
        }
    }
    fun peekChar(): Char? = source.getOrNull(currentPosition)
    fun getChar(): Char? = peekChar()?.also { currentPosition++ }

    fun readSymbolOrKeyword(): Token {
        val value = readAllowed { it.isDigit() || it.isLetter() }
        val type = if (value in keywords) TokenType.KEYWORD else TokenType.SYMBOL
        return Token(type, value)
    }

    fun readNumber(): Token {
        val value = readAllowed { it in numberChars }
        val type = if (value.contains('.')) {
            throw RuntimeException("Floating point numbers are not supported yet")
        }
        else {
            TokenType.INTEGER
        }
        return Token(type, value)
    }

    fun readOperator(): Token = Token(TokenType.OPERATOR, getChar().toString())

    private fun readAllowed(isAllowed: (Char) -> Boolean): String {
        val sb = StringBuilder()
        while(true) {
            val char = peekChar() ?: break
            if (isAllowed(char)) {
                sb.append(getChar())
            } else {
                break
            }
        }
        return sb.toString()
    }
}

