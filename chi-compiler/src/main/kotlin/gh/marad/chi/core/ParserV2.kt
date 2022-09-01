package gh.marad.chi.core

import ChiParser
import ChiParserBaseVisitor
import gh.marad.chi.core.parser2.*
import org.antlr.v4.runtime.tree.TerminalNode

internal class ParserV2(private val source: ChiSource) : ChiParserBaseVisitor<ParseAst>() {

    override fun visitProgram(ctx: ChiParser.ProgramContext): ParseAst {
        ctx.removeLastChild() // remove EOF
        val body = ctx.children.mapNotNull { it.accept(this) }
        return ParseBlock(body, getSection(source, ctx))
    }

    override fun visitTerminal(node: TerminalNode): ParseAst? {
        return TerminalReader.read(source, node)
    }

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): ParseAst {
        return PackageReader.read(source, ctx)
    }

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

    override fun visitBlock(ctx: ChiParser.BlockContext): ParseAst =
        BlockReader.read(this, source, ctx)

    override fun visitAssignment(ctx: ChiParser.AssignmentContext): ParseAst =
        AssignmentReader.read(this, source, ctx)

    override fun visitIndexedAssignment(ctx: ChiParser.IndexedAssignmentContext): ParseAst =
        AssignmentReader.readIndexed(this, source, ctx)
}