package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class IfElseReaderTest {

    @Test
    fun `parsing if-else expression`() {
        val code = "if (0) 1 else 2"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody?.shouldBeLongValue(2)
        ifElse.section?.getCode() shouldBe code
    }

    @Test
    fun `else branch should be optional`() {
        val code = "if (0) 1"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody.shouldBeNull()
        ifElse.section?.getCode() shouldBe code
    }

}