package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseNameDeclaration
import gh.marad.chi.core.parser.readers.Symbol
import gh.marad.chi.core.parser.readers.TypeNameRef
import gh.marad.chi.core.shouldBeAtom
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VariablesConversionsKtNameDeclarationTest {
    @Test
    fun `generate name declaration`() {
        val ctx = defaultContext()
        val result = convertNameDeclaration(
            ctx,
            ParseNameDeclaration(
                public = true,
                mutable = false,
                symbol = Symbol("variable", sectionA),
                typeRef = TypeNameRef("int", sectionB),
                value = LongValue(10),
                section = sectionC
            )
        )

        result.public.shouldBeTrue()
        result.mutable.shouldBeFalse()
        result.name shouldBe "variable"
        result.value.shouldBeAtom("10", Type.intType)
        result.expectedType shouldBe Type.intType
        result.enclosingScope shouldBe ctx.currentScope
        result.sourceSection shouldBe sectionC
    }
}