package gh.marad.chi.core.parser.readers

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test


class ProgramReaderTest {
    @Test
    fun `read program`() {
        val code = """
            package mod/pkg
            import othermod/otherpkg as oth
            val bar = 5
            fn foo() {}
            effect foo2()
            data Baz()
        """.trimIndent()
        val program = parseProgram(code)

        program.packageDefinition.shouldNotBeNull().should {
            it.moduleName.name shouldBe "mod"
            it.packageName.name shouldBe "pkg"
        }

        program.imports.should {
            it shouldHaveSize 1
            it[0].moduleName.name shouldBe "othermod"
            it[0].packageName.name shouldBe "otherpkg"
            it[0].packageAlias.shouldNotBeNull().alias shouldBe "oth"
            it[0].entries.shouldBeEmpty()
        }

        program.typeDefinitions.should {
            it shouldHaveSize 1
            it[0].typeName shouldBe "Baz"
        }

        program.functions.should {
            it shouldHaveSize 2
            it[0].shouldBeTypeOf<ParseFuncWithName>()
            it[1].shouldBeTypeOf<ParseEffectDefinition>()
        }

        program.topLevelCode.should {
            it shouldHaveSize 1
            it[0].shouldBeTypeOf<ParseNameDeclaration>()
                .symbol.name shouldBe "bar"
        }
    }
}

fun parseProgram(code: String): Program {
    val source = ChiSource(code)
    val charStream = CharStreams.fromString(source.code)

    val lexer = ChiLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    val visitor = ParserVisitor(source)
    return ProgramReader.read(visitor, source, parser.program())
}

