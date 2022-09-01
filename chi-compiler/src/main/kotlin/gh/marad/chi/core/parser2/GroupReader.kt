package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object GroupReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.GroupExprContext): ParseAst =
        ParseGroup(
            value = ctx.expression().accept(parser),
            getSection(source, ctx)
        )
}

data class ParseGroup(val value: ParseAst, override val section: ChiSource.Section?) : ParseAst