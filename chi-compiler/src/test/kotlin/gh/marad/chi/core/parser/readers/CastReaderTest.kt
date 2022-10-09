package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.shouldBeTypeNameRef
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class CastReaderTest {
    @Test
    fun `parse cast expr`() {
        val code = "1 as string"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val cast = ast[0].shouldBeTypeOf<ParseCast>()
        cast.value.shouldBeLongValue(1)
        cast.typeRef.shouldBeTypeNameRef("string")
        cast.section?.getCode() shouldBe code
    }

}