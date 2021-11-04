package gh.marad.chi.core

import gh.marad.chi.core.TokenType.*

fun parse(tokens: List<Token>): List<Expression> {
    val parser = Parser(tokens.toTypedArray())
    val expressions = mutableListOf<Expression>()

    while(parser.hasMore()) {
        expressions.add(parser.readExpression())
    }

    return expressions
}

data class Type(val name: String) {
    companion object {
        val i32 = Type("i32")
        val unit = Type("unit")
        val fn = Type("fn") // TODO: functions will need better types
    }
}
data class FnParam(val name: String, val type: Type, val location: Location?)

sealed interface Expression {
    val location: Location?
}
data class Atom(val value: String, val type: Type, override val location: Location? = null): Expression {
    companion object {
        fun unit(location: Location?) = Atom("()", Type.unit, location)
    }
}
data class Assignment(val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?, override val location: Location? = null): Expression
data class Fn(val parameters: List<FnParam>, val returnType: Type, val block: BlockExpression, override val location: Location? = null): Expression
data class BlockExpression(val body: List<Expression>, override val location: Location? = null): Expression
data class FnCall(val name: String, val parameters: List<Expression>, override val location: Location? = null): Expression
data class VariableAccess(val name: String, override val location: Location? = null): Expression

private class Parser(private val tokens: Array<Token>) {
    private var currentPosition: Int = 0

    fun hasMore(): Boolean = currentPosition < tokens.size

    fun readExpression(): Expression {
        val nextToken = peak()
        return when {
            nextToken.type == KEYWORD && nextToken.value in arrayListOf("val", "var") -> readAssignment()
            nextToken.type == KEYWORD && nextToken.value == "fn" -> readAnonymousFunction()
            nextToken.type == SYMBOL && peakAhead()?.let { it.type == OPERATOR && it.value == "(" } ?: false -> readFunctionCall()
            nextToken.type == SYMBOL -> readVariableAccess()
            nextToken.type == INTEGER -> readAtom()
            else -> throw UnexpectedToken(nextToken)
        }
    }

    private fun peak(): Token = tokens.getOrElse(currentPosition) {
        val previousToken = tokens[currentPosition-1]
        val previousTokenLocation = previousToken.location
        val currentLocation = previousTokenLocation.copy(column = previousTokenLocation.column + previousToken.value.length)
        throw UnexpectedEndOfFile(currentLocation)
    }
    private fun peakAhead(): Token? = tokens.getOrNull(currentPosition+1)
    private fun get(): Token =  tokens[currentPosition].also { currentPosition++ }
    private fun skip() { currentPosition++ }

    private fun readAssignment(): Assignment {
        val variableTypeToken = get()
        val immutable = when(variableTypeToken.value) {
            "val" -> true
            "var" -> false
            else -> throw OneOfTokensExpected(listOf("val", "var"), variableTypeToken)
        }
        val nameSymbol = expectSymbol()
        val expectedType = readOptionalTypeDefinition()
        expectOperator("=")
        val valueExpression = readExpression()
        return Assignment(nameSymbol.value, valueExpression, immutable, expectedType, variableTypeToken.location)
    }

    private fun readAnonymousFunction(): Fn {
        val fnKeyword = expectKeyword("fn")
        expectOperator("(")
        val parameters = mutableListOf<FnParam>()
        while(peak().value != ")") {
            parameters.add(readFunctionParameterDefinition())
            val next = peak()
            when {
                next.type == OPERATOR && next.value == "," -> skip()
                next.type == OPERATOR && next.value == ")" -> break
                else -> throw OneOfTokensExpected(listOf(",", ")"), next)
            }
        }
        expectOperator(")")
        val returnType = readOptionalTypeDefinition() ?: Type.unit
        val body = readBlockExpression()
        return Fn(parameters, returnType, body, fnKeyword.location)
    }

    private fun readFunctionParameterDefinition(): FnParam {
        val paramName = expectSymbol()
        expectOperator(":")
        val type = readType()
        return FnParam(paramName.value, type, paramName.location)
    }

    private fun readBlockExpression(): BlockExpression {
        val openBrace = expectOperator("{")
        val body = mutableListOf<Expression>()
        while(peak().value != "}") {
            body.add(readExpression())
        }
        expectOperator("}")
        return BlockExpression(body, openBrace.location)
    }

    private fun readOptionalTypeDefinition(): Type? {
        return if (peak().value == ":") {
            expectOperator(":")
            readType()
        } else {
            null
        }
    }

    private fun readType(): Type {
        val token = get()
        if (token.type !in arrayOf(SYMBOL, KEYWORD)) {
            throw UnexpectedToken(token, suggestion = "You seem to be missing type declaration or it's invalid.")
        }
        return Type(token.value)
    }

    private fun readFunctionCall(): FnCall {
        val nameSymbol = expectSymbol()
        expectOperator("(")
        val parametersExpressions = mutableListOf<Expression>()
        while(peak().value != ")") {
            parametersExpressions.add(readExpression())
            val next = peak()
            when {
                next.type == OPERATOR && next.value == "," -> skip()
                next.type == OPERATOR && next.value == ")" -> break
                else -> throw OneOfTokensExpected(listOf(",", ")"), next)
            }
        }
        expectOperator(")")
        return FnCall(nameSymbol.value, parametersExpressions, nameSymbol.location)
    }

    private fun readVariableAccess(): VariableAccess {
        val variableName = expectSymbol()
        return VariableAccess(variableName.value, variableName.location)
    }

    private fun readAtom(): Atom {
        val token = get()
        val type = Type.i32
        return Atom(token.value, type, token.location)
    }

    private fun expectOperator(operator: String): Token = expect(OPERATOR, operator)
    private fun expectKeyword(keyword: String? = null): Token = expect(KEYWORD, keyword)
    private fun expectSymbol(): Token = expect(SYMBOL)

    private fun expect(tokenType: TokenType, expectedValue: String? = null): Token =
        get()
            .also {
                if(it.type != tokenType || (expectedValue != null && it.value != expectedValue)) {
                    throw UnexpectedToken(it, expectedValue)
                }
            }
}
