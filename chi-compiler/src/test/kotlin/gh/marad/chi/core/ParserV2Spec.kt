package gh.marad.chi.core

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser2.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class ParserV2Spec : FunSpec({

    fun parse(code: String): List<ParseAst> {
        val source = ChiSource(code)
        val charStream = CharStreams.fromString(source.code)

        val lexer = ChiLexer(charStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = ChiParser(tokenStream)
        val visitor = ParserV2(source)
        val block = visitor.visitProgram(parser.program()) as ParseBlock
        return block.body
    }

    test("parse package definition") {
        val code = "package some.module/some.pkg"
        val result = parse(code)
        result shouldHaveSize 1
        result[0].shouldBeTypeOf<ParsePackageDefinition>() should {
            it.moduleName.name shouldBe "some.module"
            it.packageName.name shouldBe "some.pkg"
            it.section?.getCode() shouldBe code
        }
    }

    test("parse import definition") {
        val code = "import some.module/some.pkg as pkgAlias { foo as fooAlias, bar as barAlias }"
        val result = parse(code)
        result shouldHaveSize 1
        result[0].shouldBeTypeOf<ParseImportDefinition>() should {
            it.moduleName.name shouldBe "some.module"
            it.packageName.name shouldBe "some.pkg"
            it.packageAlias?.alias shouldBe "pkgAlias"
            it.entries shouldHaveSize 2
            it.entries[0] should { fooEntry ->
                fooEntry.name shouldBe "foo"
                fooEntry.alias?.alias shouldBe "fooAlias"
                fooEntry.section?.getCode() shouldBe "foo as fooAlias"
            }
            it.entries[1] should { barEntry ->
                barEntry.name shouldBe "bar"
                barEntry.alias?.alias shouldBe "barAlias"
                barEntry.section?.getCode() shouldBe "bar as barAlias"
            }
            it.section?.getCode() shouldBe code
        }
    }

    test("parse variant type definition") {
        val code = "data Result[V, E] = Ok(value: V) | Err(error: E)"
        val result = parse(code)
        result shouldHaveSize 1
        result[0].shouldBeTypeOf<gh.marad.chi.core.parser2.VariantTypeDefinition>() should {
            it.typeName shouldBe "Result"
            it.typeParameters.map { it.name } shouldBe listOf("V", "E")

            it.variantConstructors shouldHaveSize 2
            it.variantConstructors[0] should { constructor ->
                constructor.name shouldBe "Ok"
                constructor.formalParameters shouldHaveSize 1
                constructor.formalParameters[0].should {
                    it.name shouldBe "value"
                    it.typeRequirement.shouldBeTypeOf<TypeNameRequirement>()
                        .typeName.shouldBe("V")
                }
            }

            it.variantConstructors[1] should { constructor ->
                constructor.name shouldBe "Err"
                constructor.formalParameters shouldHaveSize 1
                constructor.formalParameters[0].should {
                    it.name shouldBe "error"
                    it.typeRequirement.shouldBeTypeOf<TypeNameRequirement>()
                        .typeName.shouldBe("E")
                }
            }
        }
    }
})