package gh.marad.chi.core

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser2.*
import io.kotest.core.spec.style.FunSpec
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
        ast[0].shouldBeTypeOf<gh.marad.chi.core.parser2.VariantTypeDefinition>() should {
            it.typeName shouldBe "Result"
            it.typeParameters.map { it.name } shouldBe listOf("V", "E")

            it.variantConstructors shouldHaveSize 2
            it.variantConstructors[0] should { constructor ->
                constructor.name shouldBe "Ok"
                constructor.formalParameters shouldHaveSize 1
                constructor.formalParameters[0].should {
                    it.name shouldBe "value"
                    it.typeRef.shouldBeTypeOf<TypeNameRef>()
                        .typeName.shouldBe("V")
                }
            }

            it.variantConstructors[1] should { constructor ->
                constructor.name shouldBe "Err"
                constructor.formalParameters shouldHaveSize 1
                constructor.formalParameters[0].should {
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
            .typeRef.shouldBeTypeOf<GenericTypeRef>()

        typeRef.typeName shouldBe "HashMap"
        typeRef.genericTypeParameters.map { it.shouldBeTypeOf<TypeNameRef>() }
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
            it.condition.shouldBeIntValue(0)
            it.body.shouldBeIntValue(1)
            it.section?.getCode() shouldBe "0 -> 1"
        }
        whenExpr.cases[1].should {
            it.condition.shouldBeIntValue(1)
            it.body.shouldBeIntValue(2)
            it.section?.getCode() shouldBe "1 -> 2"
        }
        whenExpr.elseCase.shouldNotBeNull() should {
            it.body.shouldBeIntValue(3)
            it.section?.getCode() shouldBe "else -> 3"
        }
        whenExpr.section?.getCode() shouldBe code.trim()
    }

    test("parsing group expression") {
        val code = "(1)"
        val ast = parse(code)
        ast shouldHaveSize 1
        val group = ast[0].shouldBeTypeOf<ParseGroup>()

        group.value.shouldBeIntValue(1)
        group.section?.getCode() shouldBe code
    }

    test("parsing if-else expression") {
        val code = "if (0) 1 else 2"
        val ast = parse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeIntValue(0)
        ifElse.thenBody.shouldBeIntValue(1)
        ifElse.elseBody?.shouldBeIntValue(2)
        ifElse.section?.getCode() shouldBe code
    }

    test("else is optional for if-else expression") {
        val code = "if (0) 1"
        val ast = parse(code)

        ast shouldHaveSize 1
        val ifElse = ast[0].shouldBeTypeOf<ParseIfElse>()
        ifElse.condition.shouldBeIntValue(0)
        ifElse.thenBody.shouldBeIntValue(1)
        ifElse.elseBody.shouldBeNull()
        ifElse.section?.getCode() shouldBe code
    }

    test("parsing func expression") {
        val code = "fn(a: int, b: string): unit { 0 }"
        val ast = parse(code)

        ast shouldHaveSize 1
        val func = ast[0].shouldBeTypeOf<ParseFunc>()
        func.formalParameters.should {
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
            it.body[0].shouldBeIntValue(0)
        }
        func.section?.getCode() shouldBe code
    }

    test("parsing func with name") {
        val code = "fn someName(a: int, b: string): unit { 0 }"
        val ast = parse(code)

        ast shouldHaveSize 1
        val func = ast[0].shouldBeTypeOf<ParseFuncWithName>()
        func.name shouldBe "someName"
        func.formalParameters.should {
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
            it.body[0].shouldBeIntValue(0)
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
        assignment.value.shouldBeIntValue(5)
        assignment.section?.getCode() shouldBe code
    }

    test("parsing indexed assignment") {
        val code = "x[10] = 5"
        val ast = parse(code)

        ast shouldHaveSize 1
        val assignment = ast[0].shouldBeTypeOf<ParseAssignment>()
        assignment.variableName shouldBe "x"
        assignment.index.shouldNotBeNull().shouldBeIntValue(10)
        assignment.value.shouldBeIntValue(5)
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
            it[0].shouldBeIntValue(1)
            it[1].shouldBeIntValue(2)
        }
    }
})

fun Any.shouldBeIntValue(value: Int) {
    this.shouldBeTypeOf<IntValue>().value shouldBe value
}

fun Any.shouldBeVariable(variableName: String) {
    this.shouldBeTypeOf<ParseVariableRead>().variableName shouldBe variableName
}

fun Any.shouldBeTypeNameRef(typeName: String) {
    this.shouldBeTypeOf<TypeNameRef>().typeName shouldBe typeName
}