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
                constructor.formalArguments shouldHaveSize 1
                constructor.formalArguments[0].should {
                    it.name shouldBe "value"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("V")
                }
            }

            it.variantConstructors[1] should { constructor ->
                constructor.name shouldBe "Err"
                constructor.formalArguments shouldHaveSize 1
                constructor.formalArguments[0].should {
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
                constructor.formalArguments[0].should {
                    it.name shouldBe "t"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("T")
                }
                constructor.formalArguments[1].should {
                    it.name shouldBe "u"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("U")
                }
            }
        }
    }

}