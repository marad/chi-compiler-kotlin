package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class WhenReaderTest {
    @Test
    fun `parsing when expression`() {
        val code = """
            when {
                0 -> 1
                1 -> 2
                else -> 3
            }
        """
        val ast = testParse(code)

        ast shouldHaveSize 1
        val whenExpr = ast[0].shouldBeTypeOf<ParseWhen>()
        whenExpr.cases shouldHaveSize 2
        whenExpr.cases[0].should {
            it.condition.shouldBeLongValue(0)
            it.body.shouldBeLongValue(1)
            it.section?.getCode() shouldBe "0 -> 1"
        }
        whenExpr.cases[1].should {
            it.condition.shouldBeLongValue(1)
            it.body.shouldBeLongValue(2)
            it.section?.getCode() shouldBe "1 -> 2"
        }
        whenExpr.elseCase.shouldNotBeNull() should {
            it.body.shouldBeLongValue(3)
            it.section?.getCode() shouldBe "else -> 3"
        }
        whenExpr.section?.getCode() shouldBe code.trim()
    }

    @Test
    fun `parse when expression with blocks`() {
        val code = """
            when {
                0 -> { 1 }
            }
        """.trimIndent()

        val ast = testParse(code)

        ast shouldHaveSize 1
        val whenExpr = ast[0].shouldBeTypeOf<ParseWhen>()
        whenExpr.cases shouldHaveSize 1
        whenExpr.cases[0].should {
            it.condition.shouldBeLongValue(0)
            it.body.shouldBeTypeOf<ParseBlock>()
        }
    }
}