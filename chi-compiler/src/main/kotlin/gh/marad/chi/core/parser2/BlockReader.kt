package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object BlockReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.BlockContext): ParseAst =
        ParseBlock(
            body = ctx.expression().map { it.accept(parser) },
            section = getSection(source, ctx)
        )
}

data class ParseBlock(val body: List<ParseAst>, override val section: ChiSource.Section?) : ParseAst

