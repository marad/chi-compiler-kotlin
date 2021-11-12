package gh.marad.chi.core

import ChiLexer
import ChiParser
import ChiParserBaseVisitor
import gh.marad.chi.actionast.ActionAst
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import gh.marad.chi.actionast.Program as ActionAstProgram


data class CompilationResult(
    val messages: List<Message>,
    val program: ActionAstProgram,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
}

/**
 * Compiles source code and produces compilation result that
 * contains AST and compilation messages.
 *
 * @param source Chi source code.
 * @param parentScope Optional scope, so you can add external names.
*/
fun compile(source: String, parentScope: CompilationScope? = null): CompilationResult {
    val program = parseProgram(source, parentScope)
    val messages = analyze(program)
    return CompilationResult(messages, ActionAst.from(program) as ActionAstProgram)
}

fun parseProgram(source: String, parentScope: CompilationScope? = null): Program {
    val charStream = CharStreams.fromString(source)
    val lexer = ChiLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    parser.errorHandler = DefaultErrorStrategy()
    val visitor = AntlrToAstVisitor(parentScope ?: CompilationScope())
    return visitor.visitProgram(parser.program()) as Program
}


class AntlrToAstVisitor(private var currentScope: CompilationScope = CompilationScope(mutableMapOf()))
    : ChiParserBaseVisitor<Expression>() {

    override fun visitProgram(ctx: ChiParser.ProgramContext): Expression {
        val exprs = ctx.expression().map { it.accept(this) }
        return Program(exprs)
    }
    override fun visitName_declaration(ctx: ChiParser.Name_declarationContext): Expression {
        val symbolName = ctx.ID().text
        val value = ctx.expression().accept(this)
        val immutable = ctx.VAL() != null
        val expectedType = ctx.type()?.let { readType(it) }
        val location = if (ctx.VAL() != null) {
            ctx.VAL().symbol.toLocation()
        } else {
            ctx.VAR().symbol.toLocation()
        }
        currentScope.addLocalName(symbolName, value)
        return NameDeclaration(symbolName, value, immutable, expectedType, location)
    }

    private fun readType(ctx: ChiParser.TypeContext): Type {
        return if (ctx.ID() != null) {
            SimpleType(ctx.ID().text)
        } else {
            val argTypes = ctx.type().map { readType(it) }
            val returnType = readType(ctx.func_return_type().type())
            FnType(argTypes, returnType)
        }
    }

    override fun visitFunc(ctx: ChiParser.FuncContext): Expression {
        return withNewScope {
            val fnParams = ctx.ID().zip(ctx.type()).map {
                val name = it.first.text
                val type = readType(it.second)
                currentScope.addParameter(name, type)
                FnParam(name, type, it.first.symbol.toLocation())
            }
            val returnType = ctx.func_return_type()?.type()?.let { readType(it) } ?: Type.unit
            val block = Block(ctx.expression().map { it.accept(this) }, ctx.LBRACE().symbol.toLocation())
            Fn(currentScope, fnParams, returnType, block, ctx.FN().symbol.toLocation())
        }
    }


    private fun Token.toLocation() = Location(line, charPositionInLine)

    override fun visitTerminal(node: TerminalNode): Expression {
        val location = node.symbol.toLocation()
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> {
                Atom(node.text, Type.i32, location)
            }
            ChiLexer.ID -> {
                VariableAccess(currentScope, node.text, location)
            }
            ChiLexer.TRUE -> Atom.t(location)
            ChiLexer.FALSE -> Atom.f(location)
            else -> {
                TODO("Unsupported type ${node.symbol.type}")
            }
        }
    }

    override fun visitAssignment(ctx: ChiParser.AssignmentContext): Expression {
        val name = ctx.ID().text
        val value = ctx.expression().accept(this)
        return Assignment(currentScope, name, value, ctx.EQUALS().symbol.toLocation())
    }

    override fun visitFn_call(ctx: ChiParser.Fn_callContext): Expression {
        val name = ctx.ID().text
        val parameters = ctx.expression().map { it.accept(this) }
        return FnCall(currentScope, name, parameters, ctx.ID().symbol.toLocation())
    }

    override fun visitIf_expr(ctx: ChiParser.If_exprContext): Expression {
        val condition = ctx.condition().expression().accept(this)
        val thenPart = getIfElseBlock(ctx.then_expr().expression(), ctx.LBRACE(0))!!
        val elsePart = getIfElseBlock(ctx.else_expr()?.expression(), ctx.LBRACE(1))
        return IfElse(
            condition = condition,
            thenBranch = thenPart,
            elseBranch = elsePart,
            ctx.IF().symbol.toLocation()
        )
    }

    override fun visitBinOp(ctx: ChiParser.BinOpContext): Expression {
        val opTerminal = ctx.ADD_SUB() ?: ctx.MUL_DIV()
        val op = opTerminal.text
        val left = ctx.expression(0).accept(this)
        val right = ctx.expression(1).accept(this)
        return InfixOp(op, left, right, opTerminal.symbol.toLocation())
    }

    private fun getIfElseBlock(exprs: List<ChiParser.ExpressionContext>?, brace: TerminalNode?): Block? {
        return if (exprs != null && brace != null) {
            Block(
                exprs.map { it.accept(this) },
                brace.symbol.toLocation()
            )
        } else {
            null
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


fun main() {
    val foo = compile("""
        val x = 100
        val main = fn(a: i32): i32 { a }
    """.trimIndent())

    println(foo)
}