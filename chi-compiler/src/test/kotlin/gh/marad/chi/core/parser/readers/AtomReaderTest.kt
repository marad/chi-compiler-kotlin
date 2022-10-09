package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.shouldBeVariable
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class AtomReaderTest {

    @Test
    fun `should parse an int`() {
        val code = "10"
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeLongValue(10)
        ast[0].section?.getCode() shouldBe code
    }

    @Test
    fun `should parse a float`() {
        val code = "10.5"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<FloatValue>()
        f.value shouldBe 10.5
        f.section?.getCode() shouldBe code
    }


    @Test
    fun `should parse a boolean true`() {
        val code = "true"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<BoolValue>()
        f.value shouldBe true
        f.section?.getCode() shouldBe code
    }

    @Test
    fun `should parse a boolean false`() {
        val code = "false"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<BoolValue>()
        f.value shouldBe false
        f.section?.getCode() shouldBe code
    }


    @Test
    fun `should parse an empty string`() {
        val code = "\"\""
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<StringValue>()
            .value.shouldBeEmpty()
    }

    @Test
    fun `should parse a string`() {
        val code = "\"hello world\""
        val ast = testParse(code)

        ast shouldHaveSize 1
        val s = ast[0].shouldBeTypeOf<StringValue>()
        s.value shouldBe "hello world"
        ast[0].section?.getCode() shouldBe "\"hello world\""
    }


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
        ast[0].shouldBeTypeOf<ParseInterpolatedString>() should {
            it.parts shouldHaveSize 2
            it.parts[0].shouldBeTypeOf<StringText>()
                .text shouldBe "simple "
            it.parts[1].shouldBeTypeOf<ParseInterpolation>()
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
        ast[0].shouldBeTypeOf<ParseInterpolatedString>() should {
            it.parts shouldHaveSize 2
            it.parts[0].shouldBeTypeOf<StringText>()
                .text shouldBe "simple "
            it.parts[1].shouldBeTypeOf<ParseInterpolation>()
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

    @Test
    fun `should handle escape characters`() {
        val cases = listOf(
            "\\n" to "\n",
            "\\r" to "\r",
            "\\\\" to "\\",
            "\\t" to "\t",

            )
        cases.forEach { (from, to) ->
            val code = """
                "$from"
            """.trimIndent()
            val ast = testParse(code)

            ast shouldHaveSize 1
            ast[0].shouldBeTypeOf<StringValue>()
                .value shouldBe to
        }
    }

}