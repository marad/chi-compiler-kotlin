package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.shouldBeLongValue
import gh.marad.chi.core.parser.shouldBeTypeNameRef
import gh.marad.chi.core.parser.testParse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class FuncReaderTest {
    @Test
    fun `should read internal function`() {
        val code = "fn foo() {}"
        testParse(code)[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.public shouldBe false
        }
    }

    @Test
    fun `should read public function`() {
        val code = "pub fn foo() {}"
        testParse(code)[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.public shouldBe true
        }
    }

    @Test
    fun `read function declaration with return type`() {
        val ast = testParse("fn foo(): int {}")
        ast[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.returnTypeRef.shouldNotBeNull().shouldBeTypeNameRef("int")
        }
    }

    @Test
    fun `read function body`() {
        val ast = testParse("fn foo() { 10 }")
        ast[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.body.shouldBeTypeOf<ParseBlock>() should { block ->
                block.body shouldHaveSize 1
                block.body[0].shouldBeLongValue(10)
            }
        }
    }

    @Test
    fun `read function arguments`() {
        val ast = testParse("fn foo(a: int, b: string) {}")
        ast[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.formalArguments.shouldHaveSize(2).should { argCollection ->
                val args = argCollection.toList()
                args[0].name shouldBe "a"
                args[0].typeRef.shouldBeTypeNameRef("int")
                args[1].name shouldBe "b"
                args[1].typeRef.shouldBeTypeNameRef("string")
            }
        }
    }

    @Test
    fun `read function type parameters`() {
        val ast = testParse("fn foo[A, B](a: A): B {}")
        ast[0].shouldBeTypeOf<ParseFuncWithName>() should {
            it.typeParameters.shouldHaveSize(2).should { coll ->
                val params = coll.toList()
                params[0].name shouldBe "A"
                params[1].name shouldBe "B"
            }
        }
    }

    @Test
    fun `parsing func expression`() {
        val code = "{ a: int, b: string -> 0 }"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val func = ast[0].shouldBeTypeOf<ParseLambda>()
        func.formalArguments.should {
            it shouldHaveSize 2
            it[0].name shouldBe "a"
            it[0].typeRef.shouldBeTypeOf<TypeNameRef>()
                .typeName shouldBe "int"
            it[1].name shouldBe "b"
            it[1].typeRef.shouldBeTypeOf<TypeNameRef>()
                .typeName shouldBe "string"
        }
        func.body shouldHaveSize 1
        func.body[0].shouldBeLongValue(0)
        func.section?.getCode() shouldBe code
    }

    @Test
    fun `should read anonymous function without parameters`() {
        val ast = testParse("{ 0 }")
        ast[0].shouldBeTypeOf<ParseLambda>().should {
            it.formalArguments.shouldBeEmpty()
        }
    }

    @Test
    fun `should read empty anonymous function`() {
        val ast = testParse("{}")
        ast[0].shouldBeTypeOf<ParseLambda>()
    }
}