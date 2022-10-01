package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class VariantTypeDefinitionReaderTest {

    @Test
    fun `parse variant type definition`() {
        val code = "data Result[V, E] = Ok(value: V) | Err(error: E)"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.typeName shouldBe "Result"
            it.typeParameters.map { it.name } shouldBe listOf("V", "E")

            it.variantConstructors shouldHaveSize 2
            it.variantConstructors[0] should { constructor ->
                constructor.name shouldBe "Ok"
                constructor.formalFields shouldHaveSize 1
                constructor.formalFields[0].should {
                    it.name shouldBe "value"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("V")
                }
            }

            it.variantConstructors[1] should { constructor ->
                constructor.name shouldBe "Err"
                constructor.formalFields shouldHaveSize 1
                constructor.formalFields[0].should {
                    it.name shouldBe "error"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("E")
                }
            }
        }
    }

    @Test
    fun `parse simplified variant type definition`() {
        val code = "data Test[T, U](t: T, u: U)"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.typeName shouldBe "Test"
            it.typeParameters.map { it.name } shouldBe listOf("T", "U")

            it.variantConstructors shouldHaveSize 1
            it.variantConstructors[0] should { constructor ->
                constructor.name shouldBe "Test"
                constructor.formalFields[0].should {
                    it.name shouldBe "t"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("T")
                }
                constructor.formalFields[1].should {
                    it.name shouldBe "u"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("U")
                }
            }
        }
    }

    @Test
    fun `should be able to spread constructor definition on multiple lines`() {
        val code = """
            data Test(
                a: int,
                b: int
            )
        """.trimIndent()

        val ast = testParse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.typeName shouldBe "Test"
            it.variantConstructors.shouldHaveSize(1)
        }

    }

    @Test
    fun `should read public and internal constructors`() {
        val code = """
            data Foo = pub A() | B()
        """.trimIndent()
        testParse(code)[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.variantConstructors[0].public shouldBe true
            it.variantConstructors[1].public shouldBe false
        }
    }

    @Test
    fun `should read constructor visibility in simplified definition`() {
        testParse("data Foo()")[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.variantConstructors[0].public shouldBe false
        }
        testParse("data pub Foo()")[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.variantConstructors[0].public shouldBe true
        }
    }

    @Test
    fun `reading public and non-public fields`() {
        testParse("data Foo(pub i: int, f: float)")[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.variantConstructors[0].formalFields should { fields ->
                fields[0].public shouldBe true
                fields[1].public shouldBe false
            }
        }

        testParse("data Foo = Foo(pub i: int, f: float)")[0].shouldBeTypeOf<ParseVariantTypeDefinition>() should {
            it.variantConstructors[0].formalFields should { fields ->
                fields[0].public shouldBe true
                fields[1].public shouldBe false
            }
        }
    }
}