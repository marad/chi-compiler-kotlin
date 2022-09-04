package gh.marad.chi.core

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser2.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.Headers1
import io.kotest.data.Row1
import io.kotest.data.Table1
import io.kotest.data.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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

    test("should parse an int") {
        val code = "10"
        val ast = parse(code)

        ast shouldHaveSize 1
        ast[0].shouldBeLongValue(10)
        ast[0].section?.getCode() shouldBe code
    }

    test("should parse a float") {
        val code = "10.5"
        val ast = parse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<FloatValue>()
        f.value shouldBe 10.5
        f.section?.getCode() shouldBe code
    }


    test("should parse a boolean true") {
        val code = "true"
        val ast = parse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<BoolValue>()
        f.value shouldBe true
        f.section?.getCode() shouldBe code
    }

    test("should parse a boolean false") {
        val code = "false"
        val ast = parse(code)

        ast shouldHaveSize 1
        val f = ast[0].shouldBeTypeOf<BoolValue>()
        f.value shouldBe false
        f.section?.getCode() shouldBe code
    }

    test("should parse a string") {
        val code = "\"hello world\""
        val ast = parse(code)

        ast shouldHaveSize 1
        val s = ast[0].shouldBeTypeOf<StringValue>()
        s.value shouldBe "hello world"
        ast[0].section?.getCode() shouldBe code
    }

    test("parse package definition") {
        val code = "package some.module/some.pkg"
        val ast = parse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParsePackageDefinition>() should {
            it.moduleName.name shouldBe "some.module"
            it.packageName.name shouldBe "some.pkg"
            it.section?.getCode() shouldBe code
        }
    }

    test("parse import definition") {
        val code = "import some.module/some.pkg as pkgAlias { foo as fooAlias, bar as barAlias }"
        val ast = parse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseImportDefinition>() should {
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
        val ast = parse(code)
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

    test("parse simple type name reference") {
        val code = "val x: SomeType = 0"
        val ast = parse(code)
        ast shouldHaveSize 1
        val typeRef = ast[0].shouldBeTypeOf<ParseNameDeclaration>()
            .typeRef.shouldBeTypeOf<TypeNameRef>()
        typeRef.typeName shouldBe "SomeType"
        typeRef.section?.getCode() shouldBe "SomeType"

    }

    test("parse function type reference") {
        val code = "val x: (int, string) -> unit = 0"
        val ast = parse(code)
        ast shouldHaveSize 1
        val typeRef = ast[0].shouldBeTypeOf<ParseNameDeclaration>()
            .typeRef.shouldBeTypeOf<FunctionTypeRef>()

        typeRef.argumentTypeRefs.map {
            it.shouldBeTypeOf<TypeNameRef>()
        }.map { it.typeName } shouldBe listOf("int", "string")

        typeRef.returnType.shouldBeTypeOf<TypeNameRef>()
            .typeName shouldBe "unit"

        typeRef.section?.getCode() shouldBe "(int, string) -> unit"
    }

    test("parse generic type reference") {
        val code = "val x: HashMap[string, int] = 0"
        val ast = parse(code)
        val typeRef = ast[0].shouldBeTypeOf<ParseNameDeclaration>()
            .typeRef.shouldBeTypeOf<TypeConstructorRef>()

        typeRef.baseType shouldBe "HashMap"
        typeRef.typeParameters.map { it.shouldBeTypeOf<TypeNameRef>() }
            .map { it.typeName } shouldBe listOf("string", "int")
        typeRef.section?.getCode() shouldBe "HashMap[string, int]"
    }

    test("parsing when expression") {
        val code = """
            when {
                0 -> 1
                1 -> 2
                else -> 3
            }
        """
        val ast = parse(code)

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

    test("parsing group expression") {
        val code = "(1)"
        val ast = parse(code)
        ast shouldHaveSize 1
        val group = ast[0].shouldBeTypeOf<ParseGroup>()

        group.value.shouldBeLongValue(1)
        group.section?.getCode() shouldBe code
    }

    test("parsing if-else expression") {
        val code = "if (0) 1 else 2"
        val ast = parse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody?.shouldBeLongValue(2)
        ifElse.section?.getCode() shouldBe code
    }

    test("else is optional for if-else expression") {
        val code = "if (0) 1"
        val ast = parse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeLongValue(0)
        ifElse.thenBody.shouldBeLongValue(1)
        ifElse.elseBody.shouldBeNull()
        ifElse.section?.getCode() shouldBe code
    }

    test("parsing func expression") {
        val code = "fn(a: int, b: string): unit { 0 }"
        val ast = parse(code)

        ast shouldHaveSize 1
        val func = ast[0].shouldBeTypeOf<ParseFunc>()
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

    test("parsing func with name") {
        val code = "fn someName(a: int, b: string): unit { 0 }"
        val ast = parse(code)

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
        val ast = parse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseAssignment>()
        assignment.variableName shouldBe "x"
        assignment.index.shouldBeNull()
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }

    test("parsing indexed assignment") {
        val code = "x[10] = 5"
        val ast = parse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseAssignment>()
        assignment.variableName shouldBe "x"
        assignment.index.shouldNotBeNull().shouldBeLongValue(10)
        assignment.value.shouldBeLongValue(5)
        assignment.section?.getCode() shouldBe code
    }

    test("reading variable") {
        val code = "myVariable"
        val ast = parse(code)

        ast shouldHaveSize 1
        val variableRead = ast[0].shouldBeTypeOf<ParseVariableRead>()
        variableRead.variableName shouldBe "myVariable"
        variableRead.section?.getCode() shouldBe code
    }

    test("parsing function call") {
        val code = "func[int](1, 2)"
        val ast = parse(code)

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

    test("parsing not operator") {
        val code = "!2"
        val ast = parse(code)

        ast shouldHaveSize 1
        val notOp = ast[0].shouldBeTypeOf<ParseNot>()
        notOp.value.shouldBeLongValue(2)
        notOp.section?.getCode() shouldBe code
    }

    forAll(
        Table1(
            Headers1("op"),
            listOf("+", "-", "*", "/", "%", "&&", "<", "<=", ">", ">=", "==", "!=", "||", "&", "|", ">>", "<<")
                .map(::Row1)
        )
    )
    { op ->
        test("parsing binary operator $op") {
            val code = "1 $op 2"
            val ast = parse(code)

            ast shouldHaveSize 1
            val binOp = ast[0].shouldBeTypeOf<ParseBinaryOp>()
            binOp.op shouldBe op
            binOp.left.shouldBeLongValue(1)
            binOp.right.shouldBeLongValue(2)
            binOp.section?.getCode() shouldBe code
        }
    }

    test("parse cast expr") {
        val code = "1 as string"
        val ast = parse(code)

        ast shouldHaveSize 1
        val cast = ast[0].shouldBeTypeOf<ParseCast>()
        cast.value.shouldBeLongValue(1)
        cast.typeRef.shouldBeTypeNameRef("string")
        cast.section?.getCode() shouldBe code
    }

    test("parse dot operator") {
        val code = "a.b"
        val ast = parse(code)

        ast shouldHaveSize 1
        val op = ast[0].shouldBeTypeOf<ParseDotOp>()
        op.receiver.shouldBeVariable("a")
        op.member.shouldBeVariable("b")
        op.section?.getCode() shouldBe code
    }

    test("parse while expression") {
        val code = "while 1 { 2 }"
        val ast = parse(code)

        ast shouldHaveSize 1
        val loop = ast[0].shouldBeTypeOf<ParseWhile>()
        loop.condition.shouldBeLongValue(1)
        loop.body.shouldBeTypeOf<ParseBlock>() should {
            it.body[0].shouldBeLongValue(2)
        }
        loop.section?.getCode() shouldBe code
    }

    test("parse index operator") {
        val code = "foo[0]"
        val ast = parse(code)

        ast shouldHaveSize 1
        val op = ast[0].shouldBeTypeOf<ParseIndexOperator>()
        op.variable.shouldBeVariable("foo")
        op.index.shouldBeLongValue(0)
        op.section?.getCode() shouldBe code
    }

    test("parse is operator") {
        val code = "foo is Nothing"
        val ast = parse(code)

        ast shouldHaveSize 1
        val isOp = ast[0].shouldBeTypeOf<ParseIs>()
        isOp.value.shouldBeVariable("foo")
        isOp.typeName shouldBe "Nothing"
        isOp.section?.getCode() shouldBe code
    }
})

fun Any.shouldBeLongValue(value: Int) {
    this.shouldBeTypeOf<LongValue>().value shouldBe value.toLong()
}

fun Any.shouldBeVariable(variableName: String) {
    this.shouldBeTypeOf<ParseVariableRead>().variableName shouldBe variableName
}

fun Any.shouldBeTypeNameRef(typeName: String) {
    this.shouldBeTypeOf<TypeNameRef>().typeName shouldBe typeName
}