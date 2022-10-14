package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.FieldAccess
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseFieldAccess
import gh.marad.chi.core.parser.readers.ParseIndexOperator
import gh.marad.chi.core.parser.readers.ParseVariableRead
import gh.marad.chi.core.shouldBeAtom
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

    @Test
    fun `generate index operator`() {
        val ctx = defaultContext()
        val result = convertIndexOperator(
            ctx, ParseIndexOperator(
                variable = ParseVariableRead("variable"),
                index = LongValue(10),
                section = testSection
            )
        )

        result.variable.shouldBeVariable("variable")
        result.index.shouldBeAtom("10", Type.intType)
        result.sourceSection shouldBe testSection
    }

    @Test
    fun `should generate variable access through package name`() {
        // given
        val ctx = defaultContext()
        ctx.addPackageAlias(otherModule, otherPackage, "pkg")
        ctx.addPublicSymbol(otherModule, otherPackage, "variable")

        // when
        val result = convertFieldAccess(
            ctx,
            sampleFieldAccess.copy(
                receiverName = "pkg",
                memberName = "variable"
            )
        )

        // then
        result.shouldBeTypeOf<VariableAccess>() should {
            it.name shouldBe "variable"
            it.moduleName shouldBe otherModule.name
            it.packageName shouldBe otherPackage.name
            it.definitionScope shouldBe ctx.namespace.getOrCreatePackage(
                otherModule.name, otherPackage.name
            ).scope
            it.isModuleLocal.shouldBeFalse()
        }
    }

    @Test
    fun `should generate field access`() {
        // given
        val ctx = defaultContext()
        val type = ctx.addTypeDefinition("SomeType")
        ctx.addPublicSymbol("object", type)

        // when
        val result = convertFieldAccess(
            ctx,
            sampleFieldAccess.copy(
                receiver = ParseVariableRead("object"),
                memberName = "field"
            )
        )

        // then
        result.shouldBeTypeOf<FieldAccess>() should {
            it.receiver.shouldBeVariable("object")
            it.fieldName shouldBe "field"
            it.memberSection shouldBe sectionA
            it.typeIsModuleLocal.shouldBeTrue()
        }
    }

    @Test
    fun `should generate field access with type defined in other module`() {
        // given
        val ctx = defaultContext()
        val type = ctx.addTypeDefinition(otherModule, otherPackage, "SomeType")
        ctx.addPublicSymbol("object", type)

        // when
        val result = convertFieldAccess(
            ctx,
            sampleFieldAccess.copy(
                receiver = ParseVariableRead("object"),
                memberName = "field"
            )
        )

        // then
        result.shouldBeTypeOf<FieldAccess>() should {
            it.typeIsModuleLocal.shouldBeFalse()
        }
    }

    private val sampleFieldAccess = ParseFieldAccess(
        receiverName = "object",
        receiver = ParseVariableRead("object"),
        memberName = "field",
        memberSection = sectionA,
        section = sectionB
    )

}