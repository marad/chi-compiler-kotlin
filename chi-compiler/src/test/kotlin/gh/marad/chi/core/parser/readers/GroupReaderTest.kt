package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeLongValue
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class GroupReaderTest {

    @Test
    fun `parsing group expression`() {
        val code = "(1)"
        val ast = testParse(code)
        ast shouldHaveSize 1
        val group = ast[0].shouldBeTypeOf<ParseGroup>()

        group.value.shouldBeLongValue(1)
        group.section?.getCode() shouldBe code
    }

}