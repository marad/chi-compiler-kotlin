package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeVariable
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class IsReaderTest {
    @Test
    fun `parse is operator`() {
        val code = "foo is Nothing"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val isOp = ast[0].shouldBeTypeOf<ParseIs>()
        isOp.value.shouldBeVariable("foo")
        isOp.typeName shouldBe "Nothing"
        isOp.section?.getCode() shouldBe code
    }
}