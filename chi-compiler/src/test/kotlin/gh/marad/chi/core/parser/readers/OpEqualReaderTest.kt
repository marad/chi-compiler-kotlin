package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.shouldBeVariable
import gh.marad.chi.core.parser.testParse
import io.kotest.data.Headers2
import io.kotest.data.Row2
import io.kotest.data.forAll
import io.kotest.data.table
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class OpEqualReaderTest {
    @Test
    fun `read basic op-equal operators`() {
        forAll(
            table(
                headers = Headers2("opEqual operator", "binary operator"),
                Row2("+=", "+"),
                Row2("-=", "-"),
                Row2("*=", "*"),
                Row2("/=", "/"),
            )
        ) { opEqualOperator, binaryOperator ->
            val code = "i $opEqualOperator 1"
            val ast = testParse(code)

            ast shouldHaveSize 1
            ast[0].shouldBeTypeOf<ParseAssignment>() should {
                it.variableName shouldBe "i"
                it.value.shouldBeTypeOf<ParseBinaryOp>() should {
                    it.op shouldBe binaryOperator
                    it.left.shouldBeVariable("i")
                    it.right.shouldBeLongValue(1)
                }
            }
        }
    }
}