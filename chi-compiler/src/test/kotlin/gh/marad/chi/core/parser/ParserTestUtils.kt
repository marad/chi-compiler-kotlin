package gh.marad.chi.core.parser

import gh.marad.chi.core.antlr.ChiLexer
import gh.marad.chi.core.antlr.ChiParser
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun testParse(code: String): List<ParseAst> {
    val source = ChiSource(code)
    val charStream = CharStreams.fromString(source.code)

    val lexer = ChiLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    val visitor = ParserVisitor(source)
    val block = visitor.visitProgram(parser.program()) as ParseBlock
    return block.body
}

fun ParseAst.shouldBeStringValue(value: String) {
    this.shouldBeTypeOf<StringValue>().value shouldBe value
}

fun ParseAst.shouldBeLongValue(value: Int) {
    this.shouldBeTypeOf<LongValue>().value shouldBe value.toLong()
}

fun ParseAst.shouldBeVariable(variableName: String) {
    this.shouldBeTypeOf<ParseVariableRead>().variableName shouldBe variableName
}

fun TypeRef.shouldBeTypeNameRef(typeName: String) {
    this.shouldBeTypeOf<TypeNameRef>().typeName shouldBe typeName
}