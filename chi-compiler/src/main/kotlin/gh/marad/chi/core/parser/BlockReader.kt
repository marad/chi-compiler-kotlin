package gh.marad.chi.core.parser

import ChiParser

internal object BlockReader {
    fun read(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.BlockContext): ParseAst =
        ParseBlock(
            body = ctx.expression().map { it.accept(parser) },
            section = getSection(source, ctx)
        )
}

data class ParseBlock(val body: List<ParseAst>, override val section: ChiSource.Section?) : ParseAst

