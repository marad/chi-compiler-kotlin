package gh.marad.chi.core

// TODO wydaje mi się, że KEYWORD jest niepotrzebne. SYMBOL powinien wystarczyć na tym etapie -
//  keywordy mogą rozumieć warstwy wyżej
enum class TokenType {
    KEYWORD, SYMBOL, OPERATOR, INTEGER
}

data class Location(val line: Int, val column: Int) {
    val formattedPosition = "$line:$column"
}
data class Token(val type: TokenType, val value: String, val location: Location)

/**
 * Takes chi source code and returns list of tokens.
 */
fun tokenize(source: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val tokenizer = Tokenizer(source.toCharArray())

    while(!tokenizer.isEof()) {
        val char = tokenizer.peekChar()
        when {
            char == null -> throw UnexpectedEndOfFile(tokenizer.currentLocation())
            char.isWhitespace() -> tokenizer.skipWhitespace()
            char.isLetter() -> tokens.add(tokenizer.readSymbolOrKeyword())
            char.isDigit() -> tokens.add(tokenizer.readNumber())
            char == '-' && tokenizer.peekAhead() == '>' -> tokens.add(tokenizer.readArrowOperator())
            char in operatorChars -> tokens.add(tokenizer.readOperator())
            else -> throw UnexpectedCharacter(char, tokenizer.currentLocation())
        }
    }

    return tokens
}

private val keywords = arrayListOf("val", "var", "fn", "i32", "unit")
private val numberChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
private val operatorChars = charArrayOf('=', '+', '-', '/', '*', '{', '}', '(', ')', ':', ',')


private class Tokenizer(private var source: CharArray) {
    private var currentPosition = 0
    private var line = 0
    private var column = 0
    fun isEof(): Boolean = currentPosition >= source.size
    fun skipWhitespace() {
        while(true) {
            val char = peekChar()
            if (char != null && char.isWhitespace()) {
                getChar()
            } else {
                break
            }
        }
    }
    fun peekChar(): Char? = source.getOrNull(currentPosition)
    fun peekAhead(): Char? = source.getOrNull(currentPosition+1)
    fun getChar(): Char? = peekChar()?.also {
        currentPosition++
        column++
        if (it == '\n') {
            line++
            column = 0
        }
    }

    fun readSymbolOrKeyword(): Token {
        val location = currentLocation()
        val value = readAllowed { it.isDigit() || it.isLetter() }
        val type = if (value in keywords) TokenType.KEYWORD else TokenType.SYMBOL
        return Token(type, value, location)
    }

    fun readNumber(): Token {
        val location = currentLocation()
        val value = readAllowed { it in numberChars }
        val type = if (value.contains('.')) {
            TODO("Floating point numbers are not supported yet")
        }
        else {
            TokenType.INTEGER
        }
        return Token(type, value, location)
    }

    fun readArrowOperator(): Token {
        val location = currentLocation()
        getChar() // read -
        getChar() // read >
        return Token(TokenType.OPERATOR, "->", location)
    }

    fun readOperator(): Token {
        val location = currentLocation()
        return Token(TokenType.OPERATOR, getChar().toString(), location)
    }

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

    fun currentLocation() = Location(line, column)
}

