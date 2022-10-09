package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object CastReader {
    fun readCast(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.CastContext): ParseAst =
        ParseCast(
            ctx.expression().accept(parser),
            TypeReader.readTypeRef(parser, source, ctx.type()),
            getSection(source, ctx)
        )
}

data class ParseCast(
    val value: ParseAst,
    val typeRef: TypeRef,
    override val section: ChiSource.Section?
) : ParseAst
