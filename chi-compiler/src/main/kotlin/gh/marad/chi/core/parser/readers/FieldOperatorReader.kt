package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object FieldOperatorReader {
    fun readFieldAccess(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.FieldAccessExprContext): ParseAst =
        ParseFieldAccess(
            receiverName = ctx.receiver.text,
            memberName = ctx.memberName.text,
            receiver = ctx.receiver.accept(parser),
            memberSection = getSection(source, ctx.memberName, ctx.memberName),
            section = getSection(source, ctx)
        )

    fun readFieldAssignment(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.FieldAssignmentContext): ParseAst =
        ParseFieldAssignment(
            receiverName = ctx.receiver.text,
            receiver = ctx.receiver.accept(parser),
            memberName = ctx.memberName.text,
            value = ctx.value.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseFieldAccess(
    val receiverName: String,
    val memberName: String,
    val receiver: ParseAst,
    val memberSection: ChiSource.Section?,
    override val section: ChiSource.Section?,
) : ParseAst {
    override fun children(): List<ParseAst> = listOf(receiver)
}

data class ParseFieldAssignment(
    val receiverName: String,
    val memberName: String,
    val receiver: ParseAst,
    val value: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst {
    override fun children(): List<ParseAst> = listOf(receiver, value)
}