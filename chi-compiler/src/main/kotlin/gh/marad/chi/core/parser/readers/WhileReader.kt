package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object WhileReader {
    fun readWhile(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.WhileLoopExprContext): ParseAst =
        ParseWhile(ctx.expression().accept(parser), ctx.block().accept(parser), getSection(source, ctx))

    fun readBreak(source: ChiSource, ctx: ChiParser.BreakExprContext): ParseAst =
        ParseBreak(getSection(source, ctx))

    fun readContinue(source: ChiSource, ctx: ChiParser.ContinueExprContext): ParseAst =
        ParseContinue(getSection(source, ctx))
}

data class ParseWhile(
    val condition: ParseAst,
    val body: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseBreak(override val section: ChiSource.Section?) : ParseAst

data class ParseContinue(override val section: ChiSource.Section?) : ParseAst

