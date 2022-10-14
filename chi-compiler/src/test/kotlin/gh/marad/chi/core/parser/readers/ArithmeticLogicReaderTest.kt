package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class ArithmeticLogicReaderTest {
    @Test
    fun `parsing not operator`() {
        val code = "!2"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val notOp = ast[0].shouldBeTypeOf<ParseNot>()
        notOp.value.shouldBeLongValue(2)
        notOp.section?.getCode() shouldBe code
    }

    @ParameterizedTest
    @ValueSource(strings = ["+", "-", "*", "/", "%", "&&", "<", "<=", ">", ">=", "==", "!=", "||", "&", "|", ">>", "<<"])
    fun `parsing binary operator`(op: String) {
        val code = "1 $op 2"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val binOp = ast[0].shouldBeTypeOf<ParseBinaryOp>()
        binOp.op shouldBe op
        binOp.left.shouldBeLongValue(1)
        binOp.right.shouldBeLongValue(2)
        binOp.section?.getCode() shouldBe code
    }

    @Test
    fun `should respect arithmetic operator precedence`() {
        val ast = testParse("1 + 2 * 3")[0]

        ast.shouldBeTypeOf<ParseBinaryOp>().should {
            it.op shouldBe "+"
            it.left.shouldBeLongValue(1)
            it.right.shouldBeTypeOf<ParseBinaryOp>().should { inner ->
                inner.op shouldBe "*"
                inner.left.shouldBeLongValue(2)
                inner.right.shouldBeLongValue(3)
            }
        }
    }
}