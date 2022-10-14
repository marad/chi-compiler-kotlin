package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Block
import gh.marad.chi.core.InfixOp
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseBinaryOp
import gh.marad.chi.core.parser.readers.ParseWeave
import gh.marad.chi.core.parser.readers.ParseWeavePlaceholder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class WeaveConversionsKtWeaveTest {
    @Test
    fun `should resolve placeholder to local variable and replace placeholders with it`() {
        // given
        val parseWeave = ParseWeave(
            value = LongValue(10),
            opTemplate = ParseBinaryOp("+", ParseWeavePlaceholder(sectionA), ParseWeavePlaceholder(sectionB), sectionC),
            testSection
        )

        // when
        val result = convertWeave(defaultContext(), parseWeave)
            .shouldBeTypeOf<Block>()

        // then
        val tempVarDecl = result.body.first().shouldBeTypeOf<NameDeclaration>()
        result.body[1].shouldBeTypeOf<InfixOp>() should {
            it.op shouldBe "+"
            it.left.shouldBeVariable(tempVarDecl.name)
            it.right.shouldBeVariable(tempVarDecl.name)
        }
    }
}