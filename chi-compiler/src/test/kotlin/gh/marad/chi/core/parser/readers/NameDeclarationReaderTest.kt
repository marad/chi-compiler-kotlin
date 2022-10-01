package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class NameDeclarationReaderTest {

    @Test
    fun `parse simple type name reference`() {
        val code = "val x: SomeType = 0"
        val ast = testParse(code)
        ast shouldHaveSize 1
        val typeRef = ast[0].shouldBeTypeOf<ParseNameDeclaration>()
            .typeRef.shouldBeTypeOf<TypeNameRef>()
        typeRef.typeName shouldBe "SomeType"
        typeRef.section?.getCode() shouldBe "SomeType"
    }

    @Test
    fun `parse internal name declaration`() {
        val internal = "val internal: int = 0"
        testParse(internal)[0].shouldBeTypeOf<ParseNameDeclaration>() should {
            it.public shouldBe false
        }
    }

    @Test
    fun `parse public name declaration`() {
        val internal = "pub val internal: int = 0"
        testParse(internal)[0].shouldBeTypeOf<ParseNameDeclaration>() should {
            it.public shouldBe true
        }
    }
}