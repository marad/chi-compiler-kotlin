package gh.marad.chi.core.parser

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import java.lang.Integer.max
import java.lang.Integer.min

fun mergeSections(a: ChiSource.Section, b: ChiSource.Section): ChiSource.Section {
    assert(a.source == b.source)
    return ChiSource.Section(
        source = a.source,
        start = min(a.start, b.start),
        end = max(a.end, b.end)
    )
}

fun getSection(source: ChiSource, ctx: ParserRuleContext): ChiSource.Section =
    getSection(source, ctx.start, ctx.stop ?: ctx.start)

fun getSection(source: ChiSource, start: Token, stop: Token): ChiSource.Section =
    source.getSection(
        startIndex = start.startIndex,
        endIndex = stop.stopIndex
    )

