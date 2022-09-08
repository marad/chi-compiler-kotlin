package gh.marad.chi.core.weaveexpr

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class WeaveExprSpec {
    @Test
    fun `simple parsing weave with placeholder`() {
        val code = """
            "hello" ~> str.toUpper(_)
        """.trimIndent()
        val ast = testParse(code)

        ast[0].shouldBeTypeOf<ParseWeave>() should { weave ->
            weave.value.shouldBeStringValue("hello")
            weave.opTemplate.shouldBeTypeOf<ParseFnCall>() should { call ->
                call.arguments.first().shouldBeTypeOf<ParseWeavePlaceholder>()
            }
            weave.section?.getCode() shouldBe code
        }
    }

    @Test
    fun `parsing weave chain`() {
        val code = """
            "2hello" 
                ~> str.toUpper(_)
                ~> _[0] as int
                ~> 2 + _
        """.trimIndent()
        val ast = testParse(code)

        ast[0].shouldBeTypeOf<ParseWeave>() should { weave1 ->
            val weave2 = weave1.opTemplate.shouldBeTypeOf<ParseWeave>()
            val weave3 = weave2.opTemplate.shouldBeTypeOf<ParseWeave>()
            val op1 = weave2.value
            val op2 = weave3.value
            val op3 = weave3.opTemplate

            weave1.value.shouldBeStringValue("2hello")
            op1.shouldBeTypeOf<ParseFnCall>()
            op2.shouldBeTypeOf<ParseCast>()
            op3.shouldBeTypeOf<ParseBinaryOp>()
        }
    }

    @Test
    fun `converting to expression`() {
        val code = """
            "hello" ~> str.toUpper(_)
        """.trimIndent()
        val ast = testParse(code)
        val ctx = ConversionContext(GlobalCompilationNamespace())
        val expr = convert(ctx, ast[0])

        expr.shouldBeTypeOf<FnCall>() should {
            it.parameters.first().shouldBeAtom("hello", Type.string)
        }
    }

    @Test
    fun `converting chain to expressions`() {
        val code = """
            "2hello" 
                ~> str.toUpper(_)
                ~> _[0] as int
                ~> 2 + _
        """.trimIndent()
        val ast = testParse(code)
        val ctx = ConversionContext(GlobalCompilationNamespace())
        val expr = convert(ctx, ast[0])

        expr.shouldBeTypeOf<InfixOp>() should {
            it.left.shouldBeAtom("2", Type.intType)
            it.right.shouldBeTypeOf<Cast>() should {
                it.targetType shouldBe Type.intType
                it.expression.shouldBeTypeOf<IndexOperator>() should {
                    it.index.shouldBeAtom("0", Type.intType)
                    it.variable.shouldBeTypeOf<FnCall>() should {
                        it.parameters[0].shouldBeAtom("2hello", Type.string)
                    }
                }
            }
        }
    }
}