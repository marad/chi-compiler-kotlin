package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.shouldBeLongValue
import gh.marad.chi.core.shouldBeVariable
import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class VariableReaderTest {

    @Test
    fun `reading variable`() {
        val code = "myVariable"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val variableRead = ast[0].shouldBeTypeOf<ParseVariableRead>()
        variableRead.variableName shouldBe "myVariable"
        variableRead.section?.getCode() shouldBe code
    }

    @Test
    fun `reading variable with index`() {
        val code = "myVariable[10]"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val variableRead = ast[0].shouldBeTypeOf<ParseIndexOperator>()
        variableRead.variable.shouldBeVariable("myVariable")
        variableRead.index.shouldBeLongValue(10)
        variableRead.section?.getCode() shouldBe code
    }

    @Test
    fun `parsing simple assignment`() {
        val code = "x = 5"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseAssignment>()
        assignment.variableName shouldBe "x"
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }

    @Test
    fun `parsing indexed assignment`() {
        val code = "x[10] = 5"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseIndexedAssignment>()
        assignment.variable.shouldBeVariable("x")
        assignment.index.shouldNotBeNull().shouldBeLongValue(10)
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }


}