package gh.marad.chi.core

import gh.marad.chi.core.TokenType.*
import java.util.*

/**
 * Takes list of tokens and produces abstract syntax trees for top-level expressions.
 */
//fun parse(tokens: List<Token>, globalScope: CompilationScope? = null): List<Expression> {
//    val parser = Parser(globalScope ?: CompilationScope(), tokens.toTypedArray())
//    return parser.parse()
//}

private class TokenReader(private val tokens: Array<Token>) {
    private var currentPosition = 0
    fun hasMore(): Boolean = currentPosition < tokens.size
    fun peek(): Token = tokens.getOrElse(currentPosition) {
        val previousToken = tokens[currentPosition-1]
        val previousTokenLocation = previousToken.location
        val currentLocation = previousTokenLocation.copy(column = previousTokenLocation.column + previousToken.value.length)
        throw UnexpectedEndOfFile(currentLocation)
    }
    fun peekAhead(): Token? = tokens.getOrNull(currentPosition+1)
    fun get(): Token =  tokens[currentPosition].also { currentPosition++ }
    fun skip() { currentPosition++ }
    fun isInfixOperator() = peek().value in Tokenizer.infixOperators
}

private class Parser(private var currentScope: CompilationScope = CompilationScope(mutableMapOf()),
                     tokens: Array<Token>) {
    private val tokenReader = TokenReader(tokens)

    fun parse(): List<Expression> {
        val expStack = Stack<Expression>()
        while(tokenReader.hasMore()) {
            expStack.push(readExpression())
        }
        return expStack.toList()
    }

    fun readExpression(): Expression {
        val nextToken = tokenReader.peek()
        return when {
            nextToken.type == KEYWORD && nextToken.value in arrayListOf("val", "var") -> readNameDeclaration()
            nextToken.type == KEYWORD && nextToken.value == "fn" -> readAnonymousFunction()
            nextToken.type == SYMBOL && tokenReader.peekAhead()?.let { it.type == OPERATOR && it.value == "(" } ?: false -> readFunctionCall()
            nextToken.type == SYMBOL && tokenReader.peekAhead()?.value == "=" -> readAssignment()
            nextToken.type == SYMBOL -> readVariableAccess()
            nextToken.type == INTEGER -> readAtom(Type.i32)
            nextToken.type == KEYWORD && (nextToken.value == "true" || nextToken.value == "false") -> readAtom(Type.bool)
            nextToken.type == KEYWORD && nextToken.value == "if" -> readIfExpression()
            else -> throw UnexpectedToken(nextToken)
        }
    }
    private fun readNameDeclaration(): NameDeclaration {
        val variableTypeToken = tokenReader.get()
        val immutable = when(variableTypeToken.value) {
            "val" -> true
            "var" -> false
            else -> throw OneOfTokensExpected(listOf("val", "var"), variableTypeToken)
        }
        val nameSymbol = expectSymbol()
        val expectedType = readOptionalTypeDefinition()
        expectOperator("=")
        val valueExpression = readExpression()
        currentScope.addLocalName(nameSymbol.value, valueExpression)
        return NameDeclaration(nameSymbol.value, valueExpression, immutable, expectedType, variableTypeToken.location)
    }

    private fun readAnonymousFunction(): Fn {
        return withNewScope {
            val fnKeyword = expectKeyword("fn")
            expectOperator("(")
            val parameters = mutableListOf<FnParam>()
            while(tokenReader.peek().value != ")") {
                parameters.add(readFunctionParameterDefinition())
                val next = tokenReader.peek()
                when {
                    next.type == OPERATOR && next.value == "," -> tokenReader.skip()
                    next.type == OPERATOR && next.value == ")" -> break
                    else -> throw OneOfTokensExpected(listOf(",", ")"), next)
                }
            }
            expectOperator(")")
            val returnType = readOptionalTypeDefinition() ?: Type.unit
            val body = readBlockExpression()
            Fn(currentScope, parameters, returnType, body, fnKeyword.location)
        }
    }

    private fun readFunctionParameterDefinition(): FnParam {
        val paramName = expectSymbol()
        expectOperator(":")
        val type = readType()
        currentScope.addParameter(paramName.value, type)
        return FnParam(paramName.value, type, paramName.location)
    }

    private fun readBlockExpression(): Block {
        val openBrace = expectOperator("{")
        val body = mutableListOf<Expression>()
        while(tokenReader.peek().value != "}") {
            body.add(readExpression())
        }
        expectOperator("}")
        return Block(body, openBrace.location)
    }

    private fun readOptionalTypeDefinition(): Type? {
        return if (tokenReader.peek().value == ":") {
            expectOperator(":")
            readType()
        } else {
            null
        }
    }

    private fun readType(): Type {
        val token = tokenReader.peek()
        return if (token.value == "(") {
            readFunctionType()
        } else {
            readSimpleType()
        }
    }

    private fun readFunctionType(): Type {
        expectOperator("(")
        val parameterTypes = mutableListOf<Type>()
        while(tokenReader.peek().value != ")") {
            parameterTypes.add(readType())
            val next = tokenReader.peek()
            when {
                next.type == OPERATOR && next.value == "," -> tokenReader.skip()
                next.type == OPERATOR && next.value == ")" -> break
                else -> throw OneOfTokensExpected(listOf(",", ")"), next)
            }
        }
        expectOperator(")")
        expectOperator("->")
        val returnType = readType()
        return FnType(parameterTypes, returnType)
    }

    private fun readSimpleType(): Type {
        val token = tokenReader.get()
        if (token.type !in arrayOf(SYMBOL, KEYWORD)) {
            throw UnexpectedToken(token, suggestion = "You seem to be missing type declaration or it's invalid.")
        }
        return SimpleType(token.value)
    }

    private fun readFunctionCall(): FnCall {
        val nameSymbol = expectSymbol()
        expectOperator("(")
        val parametersExpressions = mutableListOf<Expression>()
        while(tokenReader.peek().value != ")") {
            parametersExpressions.add(readExpression())
            val next = tokenReader.peek()
            when {
                next.type == OPERATOR && next.value == "," -> tokenReader.skip()
                next.type == OPERATOR && next.value == ")" -> break
                else -> throw OneOfTokensExpected(listOf(",", ")"), next)
            }
        }
        expectOperator(")")
        return FnCall(currentScope, nameSymbol.value, parametersExpressions, nameSymbol.location)
    }

    private fun readAssignment(): Assignment {
        val nameSymbol = expectSymbol()
        val eqOperator = expectOperator("=")
        val valueExpr = readExpression()
        return Assignment(currentScope, nameSymbol.value, valueExpr, eqOperator.location)
    }

    private fun readVariableAccess(): VariableAccess {
        val variableName = expectSymbol()
        return VariableAccess(currentScope, variableName.value, variableName.location)
    }

    private fun readAtom(type: SimpleType): Atom {
        val token = tokenReader.get()
        return Atom(token.value, type, token.location)
    }

    private fun readIfExpression(): IfElse {
        val ifToken = expectKeyword("if")
        expectOperator("(")
        val condition = readExpression()
        expectOperator(")")
        val thenBlock = readBlockExpression()

        val elseBranch = if (tokenReader.hasMore() && tokenReader.peek().type == KEYWORD && tokenReader.peek().value == "else") {
            expectKeyword("else")
            readBlockExpression()
        } else {
            null
        }
        return IfElse(condition, thenBlock, elseBranch, ifToken.location)
    }

    private fun expectOperator(operator: String): Token = expect(OPERATOR, operator)
    private fun expectKeyword(keyword: String? = null): Token = expect(KEYWORD, keyword)
    private fun expectSymbol(): Token = expect(SYMBOL)

    private fun expect(tokenType: TokenType, expectedValue: String? = null): Token =
        tokenReader.get()
            .also {
                if(it.type != tokenType || (expectedValue != null && it.value != expectedValue)) {
                    throw UnexpectedToken(it, expectedValue)
                }
            }

    private fun withNewScope(f: () -> Fn): Fn {
        val parentScope = currentScope
        currentScope = CompilationScope(mutableMapOf(), parentScope)
        try {
            return f()
        } finally {
            currentScope = parentScope
        }
    }
}
