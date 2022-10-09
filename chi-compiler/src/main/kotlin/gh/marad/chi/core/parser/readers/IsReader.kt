package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object IsReader {
    fun read(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.IsExprContext): ParseAst =
        ParseIs(ctx.expression().accept(parser), ctx.variantName.text, getSection(source, ctx))
}

data class ParseIs(
    val value: ParseAst,
    val typeName: String,
    override val section: ChiSource.Section?
) : ParseAst
