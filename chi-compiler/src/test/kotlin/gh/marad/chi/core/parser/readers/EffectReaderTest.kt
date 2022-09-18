package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeTypeNameRef
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class EffectReaderTest {

    @Test
    fun `reading effect definition`() {
        val code = """
            effect readString(fileName: string): string
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseEffectDefinition>() should {
            it.name shouldBe "readString"
            it.formalArguments[0].should { param ->
                param.name shouldBe "fileName"
                param.typeRef.shouldBeTypeNameRef("string")
            }
            it.returnTypeRef.shouldBeTypeNameRef("string")
        }
    }

    @Test
    fun `reading effect handler`() {
        val code = """
            handle {
                someFunc()
            } with {
                readString(fileName) -> resume(fileName)
            }
        """.trimIndent()
        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseHandle>() should {
            it.body.body[0].shouldBeTypeOf<ParseFnCall>()
            it.cases shouldHaveSize 1
            it.cases[0] should { case ->
                case.effectName shouldBe "readString"
                case.argumentNames shouldBe listOf("fileName")
                case.body.shouldBeTypeOf<ParseFnCall>()
            }
        }
    }
}