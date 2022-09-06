package gh.marad.chi.core.parser

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

fun getSection(source: ChiSource, ctx: ParserRuleContext): ChiSource.Section =
    getSection(source, ctx.start, ctx.stop ?: ctx.start)

fun getSection(source: ChiSource, start: Token, stop: Token): ChiSource.Section =
    source.getSection(
        startIndex = start.startIndex,
        endIndex = stop.stopIndex
    )

