package gh.marad.chi.core

import ChiLexer
import ChiParser
import ChiParserBaseVisitor
import gh.marad.chi.core.parser.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.tree.TerminalNode

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
        errorListener.getMessages()
    )
}

internal class AntlrToAstVisitor(private val namespace: GlobalCompilationNamespace) :
    ChiParserBaseVisitor<Expression>() {

    private val context = ParsingContext(namespace, this)

    override fun visitProgram(ctx: ChiParser.ProgramContext): Expression {
        ctx.removeLastChild() // remove EOF
        val exprs = ctx.children.mapNotNull { it.accept(this) }
        return Program(exprs)
    }

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): Expression {
        val moduleName = ctx.module_name()?.text ?: ""
        val packageName = ctx.package_name()?.text ?: ""
        context.changeCurrentPackage(moduleName, packageName)
        return Package(moduleName, packageName, makeLocation(ctx))
    }

    override fun visitImport_definition(ctx: ChiParser.Import_definitionContext): Expression {
        val import = Import(
            moduleName = ctx.module_name()?.text ?: "",
            packageName = ctx.package_name()?.text ?: "",
            packageAlias = ctx.package_import_alias()?.text,
            entries = ctx.import_entry().map { entryCtx ->
                ImportEntry(
                    name = entryCtx.import_name().text,
                    alias = entryCtx.name_import_alias()?.text,
                )
            },
            location = makeLocation(ctx),
        )
        context.imports.addImport(import)
        return import
    }

    override fun visitVariantTypeDefinition(ctx: ChiParser.VariantTypeDefinitionContext): Expression {
        return VariantTypeDefinitionReader.read(context, ctx)
    }

    override fun visitName_declaration(ctx: ChiParser.Name_declarationContext): Expression {
        val symbolName = ctx.ID().text
        val value = ctx.expression().accept(this)
        val mutable = ctx.VAR() != null
        val expectedType = ctx.type()?.let { TypeReader.read(context, it) }
        val location = makeLocation(ctx)
        return createNameDeclaration(symbolName, value, mutable, expectedType, location)
    }

    private fun createNameDeclaration(
        symbolName: String,
        value: Expression,
        mutable: Boolean,
        expectedType: Type?,
        location: Location
    ): NameDeclaration {
        val scope = if (context.currentScope.isTopLevel) {
            SymbolScope.Package
        } else {
            SymbolScope.Local
        }

        context.currentScope.addSymbol(symbolName, value.type, scope, mutable)
        return NameDeclaration(context.currentScope, symbolName, value, mutable, expectedType, location)
    }

    override fun visitMatchExpression(ctx: ChiParser.MatchExpressionContext): Expression {
        return MatchReader.read(context, ctx)
    }

    override fun visitGroupExpr(ctx: ChiParser.GroupExprContext): Expression {
        return Group(visit(ctx.expression()), makeLocation(ctx))
    }

    override fun visitFunc(ctx: ChiParser.FuncContext): Expression {
        return context.withNewScope {
            val fnParams = readFunctionParams(ctx.func_argument_definitions())
            val returnType = ctx.func_return_type()?.type()?.let { TypeReader.read(context, it) } ?: Type.unit
            val block = visitBlock(ctx.func_body().block()) as Block
            Fn(context.currentScope, emptyList(), fnParams, returnType, block, makeLocation(ctx))
        }
    }

    override fun visitFuncWithName(ctx: ChiParser.FuncWithNameContext): Expression {
        val func = context.withNewScope {
            val fnParams = readFunctionParams(ctx.func_with_name().arguments)
            val returnType =
                ctx.func_with_name().func_return_type()?.type()?.let { TypeReader.read(context, it) } ?: Type.unit
            val block = visitBlock(ctx.func_with_name().func_body().block()) as Block
            val genericTypeParameters =
                GenericsReader.readGenericTypeParameterDefinitions(ctx.func_with_name().generic_type_definitions())
            Fn(context.currentScope, genericTypeParameters, fnParams, returnType, block, makeLocation(ctx))
        }
        return createNameDeclaration(
            ctx.func_with_name().funcName.text,
            func,
            false,
            func.type,
            makeLocation(ctx)
        )
    }

    private fun readFunctionParams(ctx: ChiParser.Func_argument_definitionsContext): List<FnParam> {
        return if (ctx.argumentsWithTypes() != null) {
            ctx.argumentsWithTypes().argumentWithType().map {
                val name = it.ID().text
                val type = TypeReader.read(context, it.type())
                val location = makeLocation(it.ID().symbol, it.type().stop)
                context.currentScope.addSymbol(name, type, SymbolScope.Argument)
                FnParam(name, type, location)
            }
        } else {
            emptyList()
        }
    }

    override fun visitBlock(ctx: ChiParser.BlockContext): Expression {
        val body = ctx.expression().map { visit(it) }
        return Block(body, makeLocation(ctx))
    }

    override fun visitTerminal(node: TerminalNode): Expression? {
        return TerminalReader.read(context, node)
    }

    override fun visitAssignment(ctx: ChiParser.AssignmentContext): Expression {
        val name = ctx.ID().text
        val value = ctx.value.accept(this)
        return Assignment(context.currentScope, name, value, makeLocation(ctx))
    }

    override fun visitFnCallExpr(ctx: ChiParser.FnCallExprContext): Expression {
        val calledName = ctx.expression().text
        val function = visit(ctx.expression())
        val callTypeParameters = readCallGenericParameters(ctx.callGenericParameters())
        val parameters = ctx.expr_comma_list().expression().map { visit(it) }
        return FnCall(context.currentScope, calledName, function, callTypeParameters, parameters, makeLocation(ctx))
    }

    private fun readCallGenericParameters(ctx: ChiParser.CallGenericParametersContext?): List<Type> {
        return ctx?.type()?.map { TypeReader.read(context, it) } ?: emptyList()
    }


    override fun visitIf_expr(ctx: ChiParser.If_exprContext): Expression {
        val condition = ctx.condition.accept(this)
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
            ?: ctx.MUL()
            ?: ctx.DIV()
            ?: ctx.MOD()
            ?: ctx.and()
            ?: ctx.COMP_OP()
            ?: ctx.or()
            ?: ctx.BIT_AND()
            ?: ctx.BIT_OR()
            ?: ctx.BIT_SHL()
            ?: ctx.BIT_SHR()
        val op = opTerminal.text
        val left = ctx.expression(0).accept(this)
        val right = ctx.expression(1).accept(this)
        return InfixOp(op, left, right, makeLocation(ctx))
    }

    override fun visitCast(ctx: ChiParser.CastContext): Expression {
        val targetType = TypeReader.read(context, ctx.type())
        val expression = ctx.expression().accept(this)
        return Cast(expression, targetType, makeLocation(ctx))
    }

    override fun visitDotOp(ctx: ChiParser.DotOpContext): Expression {
        val pkg = context.imports.lookupPackage(ctx.receiver.text)
        if (pkg != null) {
            return VariableAccess(
                pkg.module,
                pkg.pkg,
                namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
                ctx.member.text,
                makeLocation(ctx)
            )
        }

        val receiver = visit(ctx.receiver)
        val member = visit(ctx.member)

        if (receiver.type.isCompositeType() && member is Assignment) {
            return FieldAssignment(
                receiver, member.name, member.value, makeLocation(ctx)
            )
        }

        return FieldAccess(
            receiver,
            ctx.member.text,
            makeLocation(ctx),
            makeLocation(ctx.member)
        )
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

    override fun visitIndexOperator(ctx: ChiParser.IndexOperatorContext): Expression {
        val variable = ctx.variable.accept(this)
        val index = ctx.index.accept(this)
        return IndexOperator(variable, index, makeLocation(ctx))
    }

    override fun visitIndexedAssignment(ctx: ChiParser.IndexedAssignmentContext): Expression {
        val variable = ctx.variable.accept(this)
        val index = ctx.index.accept(this)
        val value = ctx.value.accept(this)
        return IndexedAssignment(variable, index, value, makeLocation(ctx))
    }

    override fun visitIsExpr(ctx: ChiParser.IsExprContext): Expression {
        val value = ctx.expression().accept(this)
        val variantName = ctx.variantName.text
        return Is(value, variantName, makeLocation(ctx))
    }
}