package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeLongValue
import gh.marad.chi.core.testParse
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class WeaveReaderTest {
    @Test
    fun `parse weave expression`() {
        val ast = testParse("5 ~> _")
        ast[0].shouldBeTypeOf<ParseWeave>() should {
            it.value.shouldBeLongValue(5)
            it.opTemplate.shouldBeTypeOf<ParseWeavePlaceholder>()
        }
    }

    @Test
    fun `parse weave expression chain`() {
        val ast = testParse("5 ~> _ ~> _")
        ast[0].shouldBeTypeOf<ParseWeave>() should {
            it.value.shouldBeLongValue(5)
            it.opTemplate.shouldBeTypeOf<ParseWeave>() should {
                it.value.shouldBeTypeOf<ParseWeavePlaceholder>()
                it.opTemplate.shouldBeTypeOf<ParseWeavePlaceholder>()
            }
        }
    }
}