package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object DotOpReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.DotOpContext): ParseAst =
        ParseDotOp(
            receiver = ctx.receiver.accept(parser),
            member = ctx.member.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseDotOp(
    val receiver: ParseAst,
    val member: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst