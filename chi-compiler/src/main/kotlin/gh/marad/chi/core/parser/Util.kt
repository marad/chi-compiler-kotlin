package gh.marad.chi.core.parser

import gh.marad.chi.core.Location
import gh.marad.chi.core.LocationPoint
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

internal fun makeLocation(ctx: ParserRuleContext) =
    makeLocation(ctx.start, ctx.stop)

internal fun makeLocation(start: Token, stop: Token?) =
    Location(
        start = start.toLocationPoint(),
        end = stop?.toLocationPoint() ?: start.toLocationPoint(),
        startIndex = start.startIndex,
        endIndex = stop?.stopIndex ?: start.stopIndex
    )

internal fun Token.toLocationPoint() = LocationPoint(line, charPositionInLine)
