package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class FieldOperatorReaderTest {
    @Test
    fun `read simple field access`() {
        val ast = testParse("object.field")
        ast[0].shouldBeTypeOf<ParseFieldAccess>() should {
            it.receiver.shouldBeVariable("object")
            it.memberName shouldBe "field"
        }
    }

    @Test
    fun `read field assignment`() {
        val ast = testParse("object.field = 10")
        ast[0].shouldBeTypeOf<ParseFieldAssignment>() should {
            it.receiver.shouldBeVariable("object")
            it.memberName shouldBe "field"
            it.value.shouldBeLongValue(10)
        }
    }

    @Test
    fun `read nested field assignment`() {
        val ast = testParse("foo.bar.baz.i = 42")
        ast[0].shouldBeTypeOf<ParseFieldAssignment>() should {
            it.memberName shouldBe "i"
            it.value.shouldBeLongValue(42)

            it.receiver.shouldBeTypeOf<ParseFieldAccess>() should {
                it.memberName shouldBe "baz"
                it.receiver.shouldBeTypeOf<ParseFieldAccess>() should {
                    it.memberName shouldBe "bar"
                    it.receiver.shouldBeVariable("foo")
                }
            }
        }
    }

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
            it.receiver.shouldBeVariable("object")
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