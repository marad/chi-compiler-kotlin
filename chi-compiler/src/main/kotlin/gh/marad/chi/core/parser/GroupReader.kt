package gh.marad.chi.core.parser

import ChiParser

internal object GroupReader {
    fun read(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.GroupExprContext): ParseAst =
        ParseGroup(
            value = ctx.expression().accept(parser),
            getSection(source, ctx)
        )
}

data class ParseGroup(val value: ParseAst, override val section: ChiSource.Section?) : ParseAst