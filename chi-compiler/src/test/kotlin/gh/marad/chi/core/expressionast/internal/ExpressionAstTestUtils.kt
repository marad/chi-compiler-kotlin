package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.parser.ChiSource

val testSource = ChiSource("dummy code")
val testSection = testSource.getSection(0, 5)
val sectionA = testSource.getSection(0, 1)
val sectionB = testSource.getSection(1, 2)
val sectionC = testSource.getSection(2, 3)

