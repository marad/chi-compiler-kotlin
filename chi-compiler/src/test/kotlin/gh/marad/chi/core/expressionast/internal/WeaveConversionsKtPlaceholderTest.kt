package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Atom
import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.ParseWeavePlaceholder
import gh.marad.chi.core.shouldBeAtom
import org.junit.jupiter.api.Test

class WeaveConversionsKtPlaceholderTest {
    @Test
    fun `should resolve placeholder`() {
        // given
        val context = defaultContext()

        // when
        val result = context.withWeaveInput(Atom.int(10, sectionA)) {
            convertWeavePlaceholder(context, ParseWeavePlaceholder(testSection))
        }

        // then
        result.shouldBeAtom("10", Type.intType, sectionA)
    }
}