package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Fn
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class FunctionConversionsKtFuncWithNameTest {

    @Test
    fun `named function should be converted to name declaration`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            public = true,
            name = "funcName",
            returnTypeRef = intTypeRef,
        )

        // when
        val result = convertFuncWithName(context, funcWithName)

        // then
        val nameDecl = result.shouldBeTypeOf<NameDeclaration>()
        nameDecl.public.shouldBeTrue()
        nameDecl.mutable.shouldBeFalse()
        nameDecl.name shouldBe "funcName"
        nameDecl.value.shouldBeTypeOf<Fn>()
        nameDecl.enclosingScope shouldBe context.currentScope
    }

    @Test
    fun `when return type is not provided it is set to unit`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            returnTypeRef = null
        )

        // when
        val fn = convertFuncWithName(context, funcWithName)
            .shouldBeTypeOf<NameDeclaration>().value
            .shouldBeTypeOf<Fn>()

        // then
        fn.returnType shouldBe Type.unit
    }

    @Test
    fun `should define arguments in function scope`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            formalArguments = listOf(
                intArg("a"),
                stringArg("b")
            )
        )

        // when
        val fn = convertFuncWithName(context, funcWithName)
            .shouldBeTypeOf<NameDeclaration>().value
            .shouldBeTypeOf<Fn>()

        // then
        with(fn.fnScope) {
            getSymbol("a").shouldNotBeNull() should {
                it.type shouldBe Type.intType
                it.symbolType shouldBe SymbolType.Argument
            }
            getSymbol("b").shouldNotBeNull() should {
                it.type shouldBe Type.string
                it.symbolType shouldBe SymbolType.Argument
            }
        }
    }

    @Test
    fun `type parameters should be resolved in return type`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            typeParameters = listOf(TypeParameter("T", sectionA)),
            returnTypeRef = TypeNameRef("T", sectionB)
        )

        // when
        val fn = convertFuncWithName(context, funcWithName)
            .shouldBeTypeOf<NameDeclaration>().value
            .shouldBeTypeOf<Fn>()

        // then
        fn.returnType shouldBe Type.typeParameter("T")
    }

    @Test
    fun `type parameters should be resolved in body`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            typeParameters = listOf(TypeParameter("T", sectionA)),
            body = ParseBlock(
                listOf(
                    sampleNameDeclaration.copy(
                        typeRef = TypeNameRef("T", sectionB)
                    )
                ), testSection
            )
        )

        // when
        val fn = convertFuncWithName(context, funcWithName)
            .shouldBeTypeOf<NameDeclaration>().value
            .shouldBeTypeOf<Fn>()

        // then
        fn.body.body.first().shouldBeTypeOf<NameDeclaration>()
            .expectedType shouldBe Type.typeParameter("T")
    }

    @Test
    fun `type parameters should be resolved in arguments`() {
        // given
        val context = defaultContext()
        val funcWithName = sampleFuncWithName.copy(
            typeParameters = listOf(TypeParameter("T", sectionA)),
            formalArguments = listOf(FormalArgument("a", TypeNameRef("T", sectionB), sectionC))
        )

        // when
        val fn = convertFuncWithName(context, funcWithName)
            .shouldBeTypeOf<NameDeclaration>().value
            .shouldBeTypeOf<Fn>()

        fn.parameters.first().type shouldBe Type.typeParameter("T")
    }


    private val sampleFuncWithName = ParseFuncWithName(
        public = true,
        name = "funcName",
        typeParameters = emptyList(),
        formalArguments = emptyList(),
        returnTypeRef = intTypeRef,
        body = ParseBlock(listOf(LongValue(10)), sectionA),
        testSection
    )

    private val sampleNameDeclaration = ParseNameDeclaration(
        public = false,
        mutable = false,
        symbol = Symbol("x", sectionA),
        typeRef = TypeNameRef("int", sectionC),
        value = LongValue(10),
        sectionB
    )
}