package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.testParse
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class FuncReaderTest {
    @Test
    fun `should read internal function`() {
        val code = "fn foo() {}"
        testParse(code)[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.public shouldBe false
        }
    }

    @Test
    fun `should read public function`() {
        val code = "pub fn foo() {}"
        testParse(code)[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.public shouldBe true
        }
    }
}