package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.antlr.ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object ArithmeticLogicReader {
    fun readNot(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.NotOpContext): ParseAst =
        ParseNot(
            value = ctx.expression().accept(parser),
            section = getSection(source, ctx)
        )

    fun readBinaryOp(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.BinOpContext) =
        ParseBinaryOp(
            op = getOperator(ctx),
            left = ctx.expression(0).accept(parser),
            right = ctx.expression(1).accept(parser),
            section = getSection(source, ctx)
        )

    private fun getOperator(ctx: ChiParser.BinOpContext): String {
        val opTerminal = ctx.plusMinus()?.PLUS()
            ?: ctx.plusMinus()?.MINUS()
            ?: ctx.divMul()?.MUL()
            ?: ctx.divMul()?.DIV()
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