package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.antlr.ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object WeaveReader {
    fun read(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.WeaveExprContext): ParseAst =
        ParseWeave(
            value = ctx.input.accept(parser),
            opTemplate = ctx.template.accept(parser),
            getSection(source, ctx)
        )
}

data class ParseWeave(
    val value: ParseAst,
    val opTemplate: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseWeavePlaceholder(
    override val section: ChiSource.Section?
) : ParseAst