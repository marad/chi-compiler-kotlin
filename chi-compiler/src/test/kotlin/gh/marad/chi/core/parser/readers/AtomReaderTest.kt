package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeVariable
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class AtomReaderTest {
    @Test
    fun `reading simple string`() {
        val code = """
            "simple string"
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<StringValue>()
            .value shouldBe "simple string"
    }

    @Test
    fun `reading simple string interpolation`() {
        val code = """
            "simple ${'$'}interpolation"
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<InterpolatedString>() should {
            it.parts shouldHaveSize 2
            it.parts[0].shouldBeTypeOf<StringText>()
                .text shouldBe "simple "
            it.parts[1].shouldBeTypeOf<Interpolation>()
                .value.shouldBeVariable("interpolation")
        }
    }

    @Test
    fun `read bracketed interpolation`() {
        val code = """
            "simple ${'$'}{ obj.method() }"
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<InterpolatedString>() should {
            it.parts shouldHaveSize 2
            it.parts[0].shouldBeTypeOf<StringText>()
                .text shouldBe "simple "
            it.parts[1].shouldBeTypeOf<Interpolation>()
                .value.shouldBeTypeOf<ParseMethodInvocation>()
        }
    }

    @Test
    fun `allow to escape the dollar sign`() {
        val code = """
            "simple \${'$'}notValue"
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<StringValue>()
            .value shouldBe "simple \$notValue"
    }

    @Test
    fun `allow to escape the quote sign`() {
        val code = """
            "simple \" still string"
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<StringValue>()
            .value shouldBe "simple \" still string"
    }

}