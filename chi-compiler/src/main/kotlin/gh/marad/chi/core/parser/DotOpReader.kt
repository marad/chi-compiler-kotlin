package gh.marad.chi.core.parser

import ChiParser

internal object DotOpReader {
    fun read(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.DotOpContext): ParseAst =
        ParseDotOp(
            receiverName = ctx.receiver.text,
            memberName = ctx.member.text,
            receiver = ctx.receiver.accept(parser),
            member = ctx.member.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseDotOp(
    val receiverName: String,
    val memberName: String,
    val receiver: ParseAst,
    val member: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst