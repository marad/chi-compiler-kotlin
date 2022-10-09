package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.parser.readers.ParseVariableRead
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class VariablesConversionsKtTest {
    @Test
    fun `convert local variable read`() {
        val ctx = defaultContext()
        convertVariableRead(ctx, ParseVariableRead("variable", testSection))
            .shouldBeTypeOf<VariableAccess>().should {
                it.name shouldBe "variable"
                it.moduleName shouldBe ctx.currentModule
                it.packageName shouldBe ctx.currentPackage
                it.isModuleLocal.shouldBeTrue()
                it.definitionScope shouldBe ctx.currentScope
                it.sourceSection shouldBe testSection
            }
    }

    @Test
    fun `convert variable read from another package in the same module`() {
        // given
        val ctx = defaultContext()
        ctx.importSymbol(defaultModule, otherPackage, "variable")

        // when
        val result = convertVariableRead(ctx, ParseVariableRead("variable", null))

        // then
        result.isModuleLocal.shouldBeTrue()
        result.moduleName shouldBe defaultModule.name
        result.packageName shouldBe otherPackage.name
    }

    @Test
    fun `convert variable read from other module`() {
        // given
        val ctx = defaultContext()
        ctx.importSymbol(otherModule, defaultPackage, "variable")

        // when
        val result = convertVariableRead(ctx, ParseVariableRead("variable", null))

        // then
        result.isModuleLocal.shouldBeFalse()
        result.moduleName shouldBe otherModule.name
        result.packageName shouldBe defaultPackage.name
    }

}