package gh.marad.chi.core.astconverter.internal

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.readers.ParseAssignment
import gh.marad.chi.core.parser.readers.ParseAst
import gh.marad.chi.core.parser.readers.ParseFuncWithName
import gh.marad.chi.core.parser.readers.ParseNameDeclaration
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test

class PackageAstSorterTest {
    @Test
    fun `should sort simple value definitions`() {
        val code = """
            val a = b
            val b = 10
        """.trimIndent()

        sortCode(code) should { asts ->
            asts[0].shouldBeTypeOf<ParseNameDeclaration>()
                .name.name shouldBe "b"
            asts[1].shouldBeTypeOf<ParseNameDeclaration>()
                .name.name shouldBe "a"
        }
    }

    @Test
    fun `should sort function invocations`() {
        val code = """
            fn foo() { bar() }
            fn bar() {}
        """.trimIndent()

        sortCode(code) should { asts ->
            asts[0].shouldBeTypeOf<ParseFuncWithName>()
                .name shouldBe "bar"
            asts[1].shouldBeTypeOf<ParseFuncWithName>()
                .name shouldBe "foo"
        }
    }

    @Test
    fun `should sort function and variable declarations`() {
        val code = """
            val a = foo()
            fn foo(): int { 5 }
        """.trimIndent()

        sortCode(code) should { asts ->
            asts[0].shouldBeTypeOf<ParseFuncWithName>()
                .name shouldBe "foo"
            asts[1].shouldBeTypeOf<ParseNameDeclaration>()
                .name.name shouldBe "a"
        }
    }

    @Test
    fun `should find assignments`() {
        val code = """
            fn foo() { a = 5 }
            var a = 10
        """.trimIndent()

        sortCode(code) should { asts ->
            asts[0].shouldBeTypeOf<ParseNameDeclaration>()
                .name.name shouldBe "a"
            asts[1].shouldBeTypeOf<ParseFuncWithName>()
                .name shouldBe "foo"
        }
    }

    @Test
    fun `should preserve non-symbol-declaring nodes order`() {
        val code = """
            a = b
            var b = 10
            b = 5
        """.trimIndent()

        sortCode(code) should { asts ->
            asts[0].shouldBeTypeOf<ParseNameDeclaration>()
                .name.name shouldBe "b"
            asts[1].shouldBeTypeOf<ParseAssignment>()
                .variableName shouldBe "b"
            asts[2].shouldBeTypeOf<ParseAssignment>()
                .variableName shouldBe "a"
        }
    }

    fun sortCode(code: String): List<ParseAst> {
        return PackageAstSorter.sortAsts(testParse(code))
    }

    fun testParse(code: String): List<ParseAst> {
        val source = ChiSource(code)
        val charStream = CharStreams.fromString(source.code)

        val lexer = ChiLexer(charStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = ChiParser(tokenStream)
        val visitor = ParserVisitor(source)
        return parser.program().expression().map { it.accept(visitor) }
    }
}
