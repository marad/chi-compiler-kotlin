package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object ArithmeticReader {
    fun readNot(parser: ParserV2, source: ChiSource, ctx: ChiParser.NotOpContext): ParseAst =
        ParseNot(
            value = ctx.expression().accept(parser),
            section = getSection(source, ctx)
        )

    fun readBinaryOp(parser: ParserV2, source: ChiSource, ctx: ChiParser.BinOpContext) =
        ParseBinaryOp(
            op = getOperator(ctx),
            left = ctx.expression(0).accept(parser),
            right = ctx.expression(1).accept(parser),
            section = getSection(source, ctx)
        )

    private fun getOperator(ctx: ChiParser.BinOpContext): String {
        val opTerminal = ctx.ADD_SUB()
            ?: ctx.MUL()
            ?: ctx.DIV()
            ?: ctx.MOD()
            ?: ctx.and()
            ?: ctx.COMP_OP()
            ?: ctx.or()
            ?: ctx.BIT_AND()
            ?: ctx.BIT_OR()
            ?: ctx.BIT_SHL()
            ?: ctx.BIT_SHR()
        return opTerminal.text
    }
}

data class ParseNot(
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseBinaryOp(
    val op: String,
    val left: ParseAst,
    val right: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst