package gh.marad.chi.core

// Tokenizer
class UnexpectedEndOfFile(val location: Location) :
        RuntimeException("Unexpected end of file at line ${location.formattedPosition}")

class UnexpectedCharacter(val char: Char, val location: Location) :
        RuntimeException("Unexpected character '$char' at line ${location.formattedPosition}")

// Parser
class UnexpectedToken(val token: Token, val expected: String? = null, val suggestion: String? = null) :
        RuntimeException("Unexpected token '${token.value}' at line ${token.location.formattedPosition}.${if (expected != null) " Expected to find '$expected'." else ""}${if (suggestion != null) " Suggestion: $suggestion." else ""}")

class OneOfTokensExpected(val expected: List<String>, val actual: Token) :
        RuntimeException("Expected one of ('${expected.joinToString("', '")}') but got '${actual.value}' at line ${actual.location.formattedPosition}")