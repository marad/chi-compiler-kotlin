package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object OpEqualReader {
    fun readAssignment(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.OpEqualExprContext): ParseAst =
        ParseAssignment(
            variableName = ctx.variable.text,
            value = ParseBinaryOp(
                op = getOperator(ctx.opEqual()),
                left = ParseVariableRead(ctx.variable.text, getSection(source, ctx.variable, ctx.variable)),
                right = ctx.value.accept(parser),
                section = getSection(source, ctx)
            ),
            section = getSection(source, ctx)
        )

    private fun getOperator(ctx: ChiParser.OpEqualContext): String {
        return when {
            ctx.PLUS_EQUAL() != null -> "+"
            ctx.MINUS_EQUAL() != null -> "-"
            ctx.MUL_EQUAL() != null -> "*"
            ctx.DIV_EQUAL() != null -> "/"
            else -> TODO("Unsupported OpEqual operator: ${ctx.text}")
        }
    }
}