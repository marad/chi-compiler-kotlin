package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.Location
import gh.marad.chi.core.LocationPoint
import gh.marad.chi.core.parser.ChiSource

fun ChiSource.Section?.asLocation(): Location? {
    return this?.let {
        Location(
            start = LocationPoint(it.startLine, it.startColumn),
            end = LocationPoint(it.endLine, it.endColumn),
            startIndex = it.start,
            endIndex = it.end
        )
    }
}
