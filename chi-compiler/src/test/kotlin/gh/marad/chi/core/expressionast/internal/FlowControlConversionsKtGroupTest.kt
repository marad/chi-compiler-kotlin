package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.BoolValue
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseGroup
import gh.marad.chi.core.parser.readers.ParseIfElse
import gh.marad.chi.core.shouldBeAtom
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FlowControlConversionsKtGroupTest {
    @Test
    fun `generate group expression`() {
        convertGroup(defaultContext(), ParseGroup(LongValue(10), testSection)) should {
            it.value.shouldBeAtom("10", Type.intType)
            it.sourceSection shouldBe testSection
        }
    }

    @Test
    fun `generate simple if expression without else branch`() {
        val result = convertIfElse(
            defaultContext(), ParseIfElse(
                condition = BoolValue(true),
                thenBody = LongValue(1),
                elseBody = null,
                section = testSection
            )
        )

        result.condition.shouldBeAtom("true", Type.bool)
        result.thenBranch.shouldBeAtom("1", Type.intType)
        result.elseBranch.shouldBeNull()
        result.sourceSection shouldBe testSection
    }

    @Test
    fun `generate else branch`() {
        val result = convertIfElse(
            defaultContext(), ParseIfElse(
                condition = BoolValue(true),
                thenBody = LongValue(1),
                elseBody = LongValue(2),
                section = testSection
            )
        )

        result.elseBranch.shouldNotBeNull()
            .shouldBeAtom("2", Type.intType)
    }
}