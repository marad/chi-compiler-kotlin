package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseIs
import gh.marad.chi.core.parser.readers.ParseVariableRead
import gh.marad.chi.core.shouldBeAtom
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class SimpleConversionsKtParseIsTest {
    @Test
    fun `convert simple 'is'`() {
        // when
        val result = convertIs(
            defaultContext(),
            ParseIs(
                value = LongValue(10),
                typeName = "string",
                section = testSection
            )
        )

        // then
        result.value.shouldBeAtom("10", Type.intType)
        result.typeOrVariant shouldBe "string"
    }

    @Test
    fun `should add variant type info in scope of then branch while reading 'if' expression`() {
        // given
        val ctx = defaultContext()
        val definedType = ctx.addTypeDefinition("SomeType", constructorNames = listOf("A", "B"))
        ctx.addPublicSymbol("variable", definedType)

        val thenScope = ctx.virtualSubscope()
        val ifReadingContext = ConversionContext.IfReadingContext(
            thenScope = thenScope,
            elseScope = ctx.virtualSubscope()
        )

        // when
        ctx.withIfReadingContext(ifReadingContext) {
            convertIs(ctx, variableIsTypeCheck)
        }

        // then
        val symbol = thenScope.getSymbol("variable", ignoreOverwrites = false)
        symbol.shouldNotBeNull().should {
            it.symbolType shouldBe SymbolType.Overwrite
            it.type.shouldBeTypeOf<VariantType>()
                .variant.shouldNotBeNull().variantName shouldBe variableIsTypeCheck.typeName
        }
    }

    private val variableIsTypeCheck = ParseIs(
        value = ParseVariableRead("variable", null),
        typeName = "B",
        section = null
    )
}