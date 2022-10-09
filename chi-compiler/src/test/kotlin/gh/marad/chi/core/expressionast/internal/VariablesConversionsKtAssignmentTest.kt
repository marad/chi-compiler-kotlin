package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseAssignment
import gh.marad.chi.core.shouldBeAtom
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VariablesConversionsKtAssignmentTest {
    @Test
    fun `generate assignment`() {
        val ctx = defaultContext()
        val result = convertAssignment(
            ctx, ParseAssignment(
                variableName = "variable",
                value = LongValue(10),
                section = testSection
            )
        )

        result.name shouldBe "variable"
        result.value.shouldBeAtom("10", Type.intType)
        result.definitionScope shouldBe ctx.currentScope
        result.sourceSection shouldBe testSection
    }

}