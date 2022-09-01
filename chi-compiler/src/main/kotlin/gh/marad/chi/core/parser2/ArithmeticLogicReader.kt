package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object ArithmeticReader {
    fun readNot(parser: ParserV2, source: ChiSource, ctx: ChiParser.NotOpContext): ParseAst =
        ParseNot(
            value = ctx.expression().accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseNot(
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst