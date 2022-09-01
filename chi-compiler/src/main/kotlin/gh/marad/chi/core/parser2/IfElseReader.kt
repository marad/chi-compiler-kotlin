package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object IfElseReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.If_exprContext): ParseAst =
        ParseIfElse(
            condition = ctx.condition.accept(parser),
            thenBody = ctx.then_expr().accept(parser),
            elseBody = ctx.else_expr()?.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseIfElse(
    val condition: ParseAst,
    val thenBody: ParseAst,
    val elseBody: ParseAst?,
    override val section: ChiSource.Section?
) : ParseAst
