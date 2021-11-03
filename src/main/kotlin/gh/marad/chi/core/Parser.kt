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

enum class Type {
    i32, unit
}
data class FnParam(val name: String, val type: Type)

sealed interface Expression
data class Atom(val value: String, val type: Type): Expression
data class Assignment(val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?): Expression
data class Fn(val parameters: List<FnParam>, val returnType: Type, val body: BlockExpression): Expression
data class BlockExpression(val body: List<Expression>): Expression
data class FnCall(val name: String, val parameters: List<Expression>): Expression
data class VariableAccess(val name: String): Expression

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
            else -> throw RuntimeException("I don't know what to do with $nextToken")
        }
    }

    private fun peak(): Token = tokens[currentPosition]
    private fun peakAhead(): Token? = tokens.getOrNull(currentPosition+1)
    private fun get(): Token =  tokens[currentPosition].also { currentPosition++ }
    private fun skip() { currentPosition++ }

    private fun readAssignment(): Assignment {
        val immutable = when(get().value) {
            "val" -> true
            "var" -> false
            else -> throw RuntimeException("Expected 'val' or 'var' to define assignment")
        }
        val nameSymbol = expectSymbol()
        val expectedType = readOptionalTypeDefinition()
        expectOperator("=")
        val valueExpression = readExpression()
        return Assignment(nameSymbol.value, valueExpression, immutable, expectedType)
    }

    private fun readAnonymousFunction(): Fn {
        expectKeyword("fn")
        expectOperator("(")
        val parameters = mutableListOf<FnParam>()
        while(peak().value != ")") {
            parameters.add(readFunctionParameterDefinition())
            val next = peak()
            when {
                next.type == OPERATOR && next.value == "," -> skip()
                next.type == OPERATOR && next.value == ")" -> break
                else -> throw RuntimeException("Expected ',' or ')', but got $next")
            }
        }
        expectOperator(")")
        val returnType = readOptionalTypeDefinition() ?: Type.unit
        val body = readBlockExpression()
        return Fn(parameters, returnType, body)
    }

    private fun readFunctionParameterDefinition(): FnParam {
        val paramName = expectSymbol()
        expectOperator(":")
        val type = readType()
        return FnParam(paramName.value, type)
    }

    private fun readBlockExpression(): BlockExpression {
        expectOperator("{")
        val body = mutableListOf<Expression>()
        while(peak().value != "}") {
            body.add(readExpression())
        }
        expectOperator("}")
        return BlockExpression(body)
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
        // TODO: handle case when the type is not valid (maybe change the type to string so it can be verified later)
        return Type.valueOf(token.value)
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
                else -> throw RuntimeException("Expected ',' or ')' but got $next")
            }
        }
        expectOperator(")")
        return FnCall(nameSymbol.value, parametersExpressions)
    }

    private fun readVariableAccess(): VariableAccess {
        val variableName = expectSymbol()
        return VariableAccess(variableName.value)
    }

    private fun readAtom(): Atom {
        val token = get()
        val type = Type.i32
        return Atom(token.value, type)
    }

    private fun expectOperator(operator: String): Token = expect(OPERATOR, operator)
    private fun expectKeyword(keyword: String? = null): Token = expect(KEYWORD, keyword)
    private fun expectSymbol(): Token = expect(SYMBOL)

    private fun expect(tokenType: TokenType, value: String? = null): Token =
        get()
            .also {
                if(it.type != tokenType || (value != null && it.value != value)) {
                    throw RuntimeException("Expected $tokenType ${value ?: ""}, but got $it")
                }
            }
}
