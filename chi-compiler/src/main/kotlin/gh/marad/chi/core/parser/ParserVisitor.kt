package gh.marad.chi.core.parser

import ChiParser
import ChiParserBaseVisitor
import gh.marad.chi.core.parser.readers.*
import org.antlr.v4.runtime.tree.TerminalNode

internal class ParserVisitor(private val source: ChiSource) : ChiParserBaseVisitor<ParseAst>() {

    override fun visitProgram(ctx: ChiParser.ProgramContext): ParseAst {
        ctx.removeLastChild() // remove EOF
        val body = ctx.children.mapNotNull { it.accept(this) }
        return ParseBlock(body, getSection(source, ctx))
    }

    override fun visitTerminal(node: TerminalNode): ParseAst? =
        AtomReader.readTerminal(source, node)

    override fun visitString(ctx: ChiParser.StringContext): ParseAst =
        AtomReader.readString(source, ctx)

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): ParseAst =
        PackageReader.read(source, ctx)

    override fun visitImport_definition(ctx: ChiParser.Import_definitionContext): ParseAst =
        ImportReader.read(source, ctx)

    override fun visitVariantTypeDefinition(ctx: ChiParser.VariantTypeDefinitionContext): ParseAst =
        VariantTypeDefinitionReader.read(this, source, ctx)

    override fun visitNameDeclarationExpr(ctx: ChiParser.NameDeclarationExprContext): ParseAst =
        NameDeclarationReader.read(this, source, ctx.name_declaration())

    override fun visitWhenExpression(ctx: ChiParser.WhenExpressionContext): ParseAst =
        WhenReader.read(this, source, ctx)

    override fun visitGroupExpr(ctx: ChiParser.GroupExprContext): ParseAst =
        GroupReader.read(this, source, ctx)

    override fun visitIfExpr(ctx: ChiParser.IfExprContext): ParseAst =
        IfElseReader.read(this, source, ctx.if_expr())

    override fun visitFunc(ctx: ChiParser.FuncContext): ParseAst =
        FuncReader.readFunc(this, source, ctx)

    override fun visitFunc_with_name(ctx: ChiParser.Func_with_nameContext): ParseAst =
        FuncReader.readFuncWithName(this, source, ctx)

    override fun visitFnCallExpr(ctx: ChiParser.FnCallExprContext): ParseAst =
        FuncReader.readFnCall(this, source, ctx)

    override fun visitBlock(ctx: ChiParser.BlockContext): ParseAst =
        BlockReader.read(this, source, ctx)

    override fun visitIndexOperator(ctx: ChiParser.IndexOperatorContext): ParseAst =
        VariableReader.readVariableIndexed(this, source, ctx)

    override fun visitAssignment(ctx: ChiParser.AssignmentContext): ParseAst =
        VariableReader.readAssignment(this, source, ctx)

    override fun visitIndexedAssignment(ctx: ChiParser.IndexedAssignmentContext): ParseAst =
        VariableReader.readIndexedAssignment(this, source, ctx)

    override fun visitNotOp(ctx: ChiParser.NotOpContext): ParseAst =
        ArithmeticReader.readNot(this, source, ctx)

    override fun visitBinOp(ctx: ChiParser.BinOpContext): ParseAst =
        ArithmeticReader.readBinaryOp(this, source, ctx)

    override fun visitCast(ctx: ChiParser.CastContext): ParseAst =
        ParseCast(
            ctx.expression().accept(this),
            TypeReader.readTypeRef(this, source, ctx.type()),
            getSection(source, ctx)
        )

    override fun visitDotOp(ctx: ChiParser.DotOpContext): ParseAst =
        DotOpReader.read(this, source, ctx)

    override fun visitWhileLoopExpr(ctx: ChiParser.WhileLoopExprContext): ParseAst =
        ParseWhile(ctx.expression().accept(this), ctx.block().accept(this), getSection(source, ctx))

    override fun visitIsExpr(ctx: ChiParser.IsExprContext): ParseAst =
        ParseIs(ctx.expression().accept(this), ctx.variantName.text, getSection(source, ctx))
}