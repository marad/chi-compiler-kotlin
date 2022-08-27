package gh.marad.chi.core.parser

import ChiParser
import gh.marad.chi.core.Match
import gh.marad.chi.core.MatchCase
import gh.marad.chi.core.MatchValueName

object MatchReader {
    fun read(context: ParsingContext, ctx: ChiParser.MatchExpressionContext): Match {
        val value = ctx.toMatch.accept(context.visitor)
        val cases = ctx.matchCase().map {
            readMatchCase(context, it)
        }
        return Match(value, cases, makeLocation(ctx))
    }

    private fun readMatchCase(context: ParsingContext, ctx: ChiParser.MatchCaseContext): MatchCase {
        return MatchCase(
            variantName = ctx.variantName.text,
            valueNames = readValueNames(ctx.valueNames()),
            body = ctx.body.accept(context.visitor),
            makeLocation(ctx)
        )
    }

    private fun readValueNames(ctx: ChiParser.ValueNamesContext?): List<MatchValueName> {
        return ctx?.valueName()?.map { MatchValueName(it.text, makeLocation(ctx)) } ?: emptyList()
    }
}