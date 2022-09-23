package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeLongValue
import gh.marad.chi.core.shouldBeStringValue
import gh.marad.chi.core.shouldBeTypeNameRef
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class FieldOperatorReaderTest {
    @Test
    fun `read method invocation`() {
        val code = """
            object.method()
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseMethodInvocation>() should {
            it.methodName shouldBe "method"
            it.arguments shouldBe emptyList()
            it.concreteTypeParameters shouldBe emptyList()
            it.receiver.shouldBeTypeOf<ParseVariableRead>()
        }
    }

    @Test
    fun `should read method arguments`() {
        val code = """
            object.method(1, "a")
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseMethodInvocation>() should {
            it.arguments[0].shouldBeLongValue(1)
            it.arguments[1].shouldBeStringValue("a")
        }
    }

    @Test
    fun `should read method type parameters`() {
        val code = """
            object.method[string]()
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseMethodInvocation>() should {
            it.concreteTypeParameters[0].shouldBeTypeNameRef("string")
        }

    }
}