package gh.marad.chi.core

import ChiLexer
import ChiParser
import ChiParserBaseVisitor
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode


data class CompilationResult(
    val messages: List<Message>,
    val program: Program,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
}

object Compiler {
    /**
     * Compiles source code and produces compilation result that
     * contains AST and compilation messages.
     *
     * @param source Chi source code.
     * @param namespace Namespace to use for compilation
     */
    @JvmStatic
    fun compile(source: String, namespace: GlobalCompilationNamespace): CompilationResult {
        val (program, parsingMessages) = parseProgram(source, namespace)
        val messages = analyze(program)
        return CompilationResult(parsingMessages + messages, program)
    }

    @JvmStatic
    fun formatCompilationMessage(source: String, message: Message): String {
        val location = message.location
        val sb = StringBuilder()
        if (location != null) {
            val sourceLine = source.lines()[location.start.line - 1]
            sb.appendLine(sourceLine)
            repeat(location.start.column) { sb.append(' ') }
            sb.append("^ ")
        }
        sb.append(message.message)
        return sb.toString()
    }
}

internal fun parseProgram(source: String, namespace: GlobalCompilationNamespace): Pair<Program, List<Message>> {
    val errorListener = MessageCollectingErrorListener()
    val charStream = CharStreams.fromString(source)
    val lexer = ChiLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(errorListener)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    parser.errorHandler = DefaultErrorStrategy()
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)
    val visitor = AntlrToAstVisitor(namespace)
    val program = if (errorListener.getMessages().isNotEmpty()) {
        Program(emptyList())
    } else {
        visitor.visitProgram(parser.program()) as Program
    }
    return Pair(
        automaticallyCastCompatibleTypes(program) as Program,
        errorListener.getMessages())
}


internal class AntlrToAstVisitor(private val namespace: GlobalCompilationNamespace)
    : ChiParserBaseVisitor<Expression>() {

    private var currentScope = namespace.getDefaultScope()
    private var currentModule = CompilationDefaults.defaultModule
    private var currentPackage = CompilationDefaults.defaultPacakge

    override fun visitProgram(ctx: ChiParser.ProgramContext): Expression {
        ctx.removeLastChild() // remove EOF
        val exprs = ctx.children.map { it.accept(this) }
        return Program(exprs)
    }

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): Expression {
        val moduleName = ctx.module_name()?.text ?: ""
        val packageName = ctx.package_name()?.text ?: ""
        currentScope = namespace.getOrCreatePackageScope(moduleName, packageName)
        currentModule = moduleName
        currentPackage = packageName
        return Package(moduleName, packageName, makeLocation(ctx))
    }

    override fun visitName_declaration(ctx: ChiParser.Name_declarationContext): Expression {
        val symbolName = ctx.ID().text
        val value = ctx.expression().accept(this)
        val immutable = ctx.VAL() != null
        val expectedType = ctx.type()?.let { readType(it) }
        val location = makeLocation(ctx)
        currentScope.addSymbol(symbolName, value.type, SymbolScope.Local)
        return NameDeclaration(currentScope, symbolName, value, immutable, expectedType, location)
    }

    private fun readType(ctx: ChiParser.TypeContext): Type {
        val primitiveType = ctx.ID()?.let { maybePrimitiveType(it.text) }
        return if (primitiveType != null){
            return primitiveType
        } else {
            val argTypes = ctx.type().map { readType(it) }
            val returnType = readType(ctx.func_return_type().type())
            FnType(argTypes, returnType)
        }
    }

    private fun maybePrimitiveType(name: String): Type? = Type.primitiveTypes.find { it.name == name }

    override fun visitGroupExpr(ctx: ChiParser.GroupExprContext): Expression {
        return Group(visit(ctx.expression()), makeLocation(ctx))
    }

    override fun visitFunc(ctx: ChiParser.FuncContext): Expression {
        return withNewScope {
            val fnParams = ctx.ID().zip(ctx.type()).map {
                val name = it.first.text
                val type = readType(it.second)
                val location = makeLocation(it.first.symbol, it.second.stop)
                    currentScope.addSymbol(name, type, SymbolScope.Argument)
                FnParam(name, type, location)
            }
            val returnType = ctx.func_return_type()?.type()?.let { readType(it) } ?: Type.unit
            val block = visitBlockWithScope(ctx.func_body().block(), currentScope)
            Fn(currentScope, fnParams, returnType, block, makeLocation(ctx))
        }
    }

    override fun visitBlock(ctx: ChiParser.BlockContext): Expression {
//        return withNewScope {
//            visitBlockWithScope(ctx, currentScope)
//        }
        return visitBlockWithScope(ctx, currentScope)
    }

    private fun visitBlockWithScope(ctx: ChiParser.BlockContext, scope: CompilationScope): Block {
        val body = ctx.expression().map { visit(it) }
        return Block(body, makeLocation(ctx))
    }

    private fun Token.toLocationPoint() = LocationPoint(line, charPositionInLine)

    private fun makeLocation(ctx: ParserRuleContext) =
        makeLocation(ctx.start, ctx.stop)

    private fun makeLocation(start: Token, stop: Token) =
        Location(
            start = start.toLocationPoint(),
            end = stop.toLocationPoint(),
            startIndex = start.startIndex,
            endIndex = stop.stopIndex
        )

    override fun visitFully_qualified_name(ctx: ChiParser.Fully_qualified_nameContext): Expression {
        val moduleName = ctx.module_name()?.text ?: currentModule
        val packageName = ctx.package_name()?.text ?: currentPackage
        val variableName = ctx.ID().text
        return VariableAccess(moduleName, packageName, namespace.getOrCreatePackageScope(moduleName, packageName), variableName, makeLocation(ctx))
    }

    override fun visitTerminal(node: TerminalNode): Expression {

        val location = makeLocation(node.symbol, node.symbol)
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> {
                if (node.text.contains(".")) {
                    Atom(node.text, Type.floatType, location)
                } else {
                    Atom(node.text, Type.intType, location)
                }
            }
            ChiLexer.ID -> {
                VariableAccess(currentModule, currentPackage, currentScope, node.text, location)
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
        return Assignment(currentScope, name, value, makeLocation(ctx))
    }

    override fun visitFnCallExpr(ctx: ChiParser.FnCallExprContext): Expression {
        val calledName = ctx.expression().text
        val function = visit(ctx.expression())
        val parameters = ctx.expr_comma_list().expression().map { visit(it) }
        return FnCall(currentScope, calledName, function, parameters, makeLocation(ctx))
    }


    override fun visitIf_expr(ctx: ChiParser.If_exprContext): Expression {
        val condition = ctx.condition().expression().accept(this)
        val thenPart = visit(ctx.then_expr().expression())
        val elsePart = ctx.else_expr()?.expression()?.let { visit(it) }
        return IfElse(
            condition = condition,
            thenBranch = thenPart,
            elseBranch = elsePart,
            makeLocation(ctx)
        )
    }

    override fun visitNotOp(ctx: ChiParser.NotOpContext): Expression {
        val opTerminal = ctx.NOT()
        val expr = ctx.expression().accept(this)
        return PrefixOp(opTerminal.text, expr, makeLocation(ctx))
    }

    override fun visitBinOp(ctx: ChiParser.BinOpContext): Expression {
        val opTerminal = ctx.ADD_SUB()
            ?: ctx.MUL_DIV()
            ?: ctx.MOD()
            ?: ctx.AND()
            ?: ctx.COMP_OP()
            ?: ctx.OR()
        val op = opTerminal.text
        val left = ctx.expression(0).accept(this)
        val right = ctx.expression(1).accept(this)
        return InfixOp(op, left, right, makeLocation(ctx))
    }

    override fun visitCast(ctx: ChiParser.CastContext): Expression {
        val targetType = readType(ctx.type())
        val expression = ctx.expression().accept(this)
        return Cast(expression, targetType, makeLocation(ctx))
    }

    override fun visitString(ctx: ChiParser.StringContext): Expression {
        val value = ctx.string_part().joinToString("") { it.text }
        return Atom.string(value, makeLocation(ctx))
    }

    override fun visitWhileLoopExpr(ctx: ChiParser.WhileLoopExprContext): Expression {
        val condition = visit(ctx.expression())
        val loop = visit(ctx.block())
        return WhileLoop(condition, loop, makeLocation(ctx))
    }

    private fun <T> withNewScope(f: () -> T): T {
        val parentScope = currentScope
        currentScope = CompilationScope(parentScope)
        try {
            return f()
        } finally {
            currentScope = parentScope
        }
    }
}
