package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.FormalArgument
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseLambda
import gh.marad.chi.core.shouldBeAtom
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FunctionConversionsKtLambdaTest {
    @Test
    fun `should generate fn definition form lambda`() {
        // given
        val context = defaultContext()
        val lambda = ParseLambda(
            formalArguments = emptyList(),
            body = listOf(LongValue(10)),
            testSection
        )

        // when
        val fn = convertLambda(context, lambda)

        // then
        fn.parameters.shouldBeEmpty()
        fn.returnType shouldBe Type.intType
        fn.body.body[0].shouldBeAtom("10", Type.intType)
        fn.fnScope.shouldNotBeNull()
        fn.sourceSection shouldBe testSection
    }

    @Test
    fun `should transfer function parameters from lambda`() {
        // given
        val context = defaultContext()
        val lambda = sampleLambda.copy(
            formalArguments = listOf(FormalArgument("name", intTypeRef, sectionB))
        )

        // when
        val fn = convertLambda(context, lambda)

        // then
        fn.parameters should {
            it shouldHaveSize 1
            it[0].name shouldBe "name"
            it[0].type shouldBe Type.intType
            it[0].sourceSection shouldBe sectionB
        }
    }

    @Test
    fun `lambda with empty body will return unit type`() {
        // given
        val context = defaultContext()
        val lambda = sampleLambda.copy(body = emptyList())

        // when
        val fn = convertLambda(context, lambda)

        // then
        fn.returnType shouldBe Type.unit

    }

    @Test
    fun `function scope should have arguments defined`() {
        // given
        val context = defaultContext()
        val lambda = sampleLambda.copy(
            formalArguments = listOf(intArg("a"), stringArg("b"))
        )

        // when
        val fn = convertLambda(context, lambda)

        // then
        with(fn.fnScope) {
            getSymbol("a").shouldNotBeNull() should { a ->
                a.scopeType shouldBe ScopeType.Function
                a.symbolType shouldBe SymbolType.Argument
                a.type shouldBe Type.intType
            }
            getSymbol("b").shouldNotBeNull() should { b ->
                b.scopeType shouldBe ScopeType.Function
                b.symbolType shouldBe SymbolType.Argument
                b.type shouldBe Type.string
            }
        }
    }

    private val sampleLambda = ParseLambda(
        formalArguments = emptyList(),
        body = emptyList(),
        testSection
    )

}