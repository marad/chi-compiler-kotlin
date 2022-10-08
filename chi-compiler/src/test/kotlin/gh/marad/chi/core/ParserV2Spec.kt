package gh.marad.chi.core

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.readers.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
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


@Suppress("unused")
class ParserV2Spec : FunSpec({

    test("parsing when expression") {
        val code = """
            when {
                0 -> 1
                1 -> 2
                else -> 3
            }
        """
        val ast = testParse(code)

        ast shouldHaveSize 1
        val whenExpr = ast[0].shouldBeTypeOf<ParseWhen>()
        whenExpr.cases shouldHaveSize 2
        whenExpr.cases[0].should {
            it.condition.shouldBeLongValue(0)
            it.body.shouldBeLongValue(1)
            it.section?.getCode() shouldBe "0 -> 1"
        }
        whenExpr.cases[1].should {
            it.condition.shouldBeLongValue(1)
            it.body.shouldBeLongValue(2)
            it.section?.getCode() shouldBe "1 -> 2"
        }
        whenExpr.elseCase.shouldNotBeNull() should {
            it.body.shouldBeLongValue(3)
            it.section?.getCode() shouldBe "else -> 3"
        }
        whenExpr.section?.getCode() shouldBe code.trim()
    }

    test("parse when expression with blocks") {
        val code = """
            when {
                0 -> { 1 }
            }
        """.trimIndent()

        val ast = testParse(code)

        ast shouldHaveSize 1
        val whenExpr = ast[0].shouldBeTypeOf<ParseWhen>()
        whenExpr.cases shouldHaveSize 1
        whenExpr.cases[0].should {
            it.condition.shouldBeLongValue(0)
            it.body.shouldBeTypeOf<ParseBlock>()
        }
    }

    test("parsing group expression") {
        val code = "(1)"
        val ast = testParse(code)
        ast shouldHaveSize 1
        val group = ast[0].shouldBeTypeOf<ParseGroup>()

        group.value.shouldBeLongValue(1)
        group.section?.getCode() shouldBe code
    }

    test("parsing if-else expression") {
        val code = "if (0) 1 else 2"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody?.shouldBeLongValue(2)
        ifElse.section?.getCode() shouldBe code
    }

    test("else is optional for if-else expression") {
        val code = "if (0) 1"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody.shouldBeNull()
        ifElse.section?.getCode() shouldBe code
    }

    test("parsing func expression") {
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

    test("parsing func with name") {
        val code = "fn someName(a: int, b: string): unit { 0 }"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val func = ast[0].shouldBeTypeOf<ParseFuncWithName>()
        func.name shouldBe "someName"
        func.formalArguments.should {
            it shouldHaveSize 2
            it[0].name shouldBe "a"
            it[0].typeRef.shouldBeTypeOf<TypeNameRef>()
                .typeName shouldBe "int"
            it[1].name shouldBe "b"
            it[1].typeRef.shouldBeTypeOf<TypeNameRef>()
                .typeName shouldBe "string"
        }
        func.returnTypeRef.shouldBeTypeOf<TypeNameRef>()
            .typeName shouldBe "unit"
        func.body.shouldBeTypeOf<ParseBlock>() should {
            it.body shouldHaveSize 1
            it.body[0].shouldBeLongValue(0)
        }
        func.section?.getCode() shouldBe code
    }

    test("parsing simple assignment") {
        val code = "x = 5"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseAssignment>()
        assignment.variableName shouldBe "x"
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }

    test("parsing indexed assignment") {
        val code = "x[10] = 5"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseIndexedAssignment>()
        assignment.variable.shouldBeVariable("x")
        assignment.index.shouldNotBeNull().shouldBeLongValue(10)
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }

    test("reading variable") {
        val code = "myVariable"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val variableRead = ast[0].shouldBeTypeOf<ParseVariableRead>()
        variableRead.variableName shouldBe "myVariable"
        variableRead.section?.getCode() shouldBe code
    }

    test("parsing function call") {
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

    test("parse cast expr") {
        val code = "1 as string"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val cast = ast[0].shouldBeTypeOf<ParseCast>()
        cast.value.shouldBeLongValue(1)
        cast.typeRef.shouldBeTypeNameRef("string")
        cast.section?.getCode() shouldBe code
    }

    test("parse dot operator") {
        val code = "a.b"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val op = ast[0].shouldBeTypeOf<ParseFieldAccess>()
        op.receiver.shouldBeVariable("a")
        op.memberName.shouldBe("b")
        op.section?.getCode() shouldBe code
    }

    test("parse while expression") {
        val code = "while 1 { 2 }"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val loop = ast[0].shouldBeTypeOf<ParseWhile>()
        loop.condition.shouldBeLongValue(1)
        loop.body.shouldBeTypeOf<ParseBlock>() should {
            it.body[0].shouldBeLongValue(2)
        }
        loop.section?.getCode() shouldBe code
    }

    test("parse break expr") {
        val code = "break"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseBreak>()
    }

    test("parse continue expr") {
        val code = "continue"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseContinue>()
    }

    test("parse index operator") {
        val code = "foo[0]"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val op = ast[0].shouldBeTypeOf<ParseIndexOperator>()
        op.variable.shouldBeVariable("foo")
        op.index.shouldBeLongValue(0)
        op.section?.getCode() shouldBe code
    }

    test("parse is operator") {
        val code = "foo is Nothing"
        val ast = testParse(code)

        ast shouldHaveSize 1
        val isOp = ast[0].shouldBeTypeOf<ParseIs>()
        isOp.value.shouldBeVariable("foo")
        isOp.typeName shouldBe "Nothing"
        isOp.section?.getCode() shouldBe code
    }
})

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