package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.shouldBeTypeNameRef
import gh.marad.chi.core.parser.shouldBeVariable
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class FuncReaderFunctionCallTest {
    @Test
    fun `parsing function call`() {
        val code = "func[int](1, 2)"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val call = ast[0].shouldBeTypeOf<ParseFnCall>()
        call.function.shouldBeVariable("func")
        call.concreteTypeParameters should {
            it shouldHaveSize 1
            it[0].shouldBeTypeNameRef("int")
        }
        call.arguments should {
            it shouldHaveSize 2
            it[0].shouldBeLongValue(1)
            it[1].shouldBeLongValue(2)
        }
    }
}