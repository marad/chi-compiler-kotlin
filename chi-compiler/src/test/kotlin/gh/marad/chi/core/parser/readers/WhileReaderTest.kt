package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeLongValue
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class WhileReaderTest {
    @Test
    fun `parse while expression`() {
        val code = "while 1 { 2 }"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val loop = ast[0].shouldBeTypeOf<ParseWhile>()
        loop.condition.shouldBeLongValue(1)
        loop.body.shouldBeTypeOf<ParseBlock>() should {
            it.body[0].shouldBeLongValue(2)
        }
        loop.section?.getCode() shouldBe code
    }

    @Test
    fun `parse break expr`() {
        val code = "break"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseBreak>()
    }

    @Test
    fun `parse continue expr`() {
        val code = "continue"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseContinue>()
    }

}