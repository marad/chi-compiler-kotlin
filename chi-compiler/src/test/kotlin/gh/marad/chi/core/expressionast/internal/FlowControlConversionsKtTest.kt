package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class FlowControlConversionsKtTest {
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

    @Test
    fun `generate if-else series from when syntax`() {
        // when
        val result = convertWhen(
            defaultContext(),
            ParseWhen(
                cases = listOf(
                    ParseWhenCase(condition = BoolValue(true), body = LongValue(1), sectionA),
                    ParseWhenCase(condition = BoolValue(false), body = LongValue(2), sectionB)
                ),
                elseCase = ParseElseCase(LongValue(0), sectionC),
                testSection
            )
        )

        // then
        result.condition.shouldBeAtom("true", Type.bool)
        result.thenBranch.shouldBeAtom("1", Type.intType)
        result.sourceSection shouldBe sectionA
        result.elseBranch.shouldBeTypeOf<IfElse>().should {
            it.condition.shouldBeAtom("false", Type.bool)
            it.thenBranch.shouldBeAtom("2", Type.intType)
            it.sourceSection shouldBe sectionB
            it.elseBranch.shouldNotBeNull().shouldBeAtom("0", Type.intType)
        }
    }

    @Test
    fun `else case is optional in when`() {
        // given
        val result = convertWhen(
            defaultContext(),
            ParseWhen(
                cases = listOf(
                    ParseWhenCase(condition = BoolValue(true), body = LongValue(1), sectionA),
                    ParseWhenCase(condition = BoolValue(false), body = LongValue(2), sectionB)
                ),
                elseCase = null,
                testSection
            )
        )

        // then
        result.elseBranch.shouldBeTypeOf<IfElse>()
            .elseBranch.shouldBeNull()
    }

    @Test
    fun `generate while`() {
        // when
        val result =
            convertWhile(defaultContext(), ParseWhile(condition = BoolValue(true), body = LongValue(1), testSection))

        // then
        result.condition.shouldBeAtom("true", Type.bool)
        result.loop.shouldBeAtom("1", Type.intType)
        result.sourceSection shouldBe testSection
    }

    @Test
    fun `generate break`() {
        convertBreak(ParseBreak(testSection))
            .shouldBeTypeOf<Break>()
            .sourceSection shouldBe testSection
    }

    @Test
    fun `generate continue`() {
        convertContinue(ParseContinue(testSection))
            .shouldBeTypeOf<Continue>()
            .sourceSection shouldBe testSection
    }
}