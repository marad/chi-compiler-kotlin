package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.LongValue
import gh.marad.chi.core.parser.readers.ParseMethodInvocation
import gh.marad.chi.core.parser.readers.ParseVariableRead
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class VariablesConversionsKtTest {
    @Test
    fun `conversion of simple method invocation`() {
        // given object of type int and method of type (int, int) -> int
        val namespace = GlobalCompilationNamespace()
        val ctx = ConversionContext(namespace)
        ctx.currentScope.addSymbol("object", Type.intType, SymbolType.Local)
        ctx.currentScope.addSymbol("method", Type.fn(Type.intType, Type.intType, Type.intType), SymbolType.Local)

        // when
        val expr = convertMethodInvocation(
            ctx, ParseMethodInvocation(
                receiverName = "object",
                methodName = "method",
                receiver = ParseVariableRead("object", null),
                concreteTypeParameters = emptyList(),
                arguments = listOf(LongValue(10, null)),
                memberSection = null,
                section = null
            )
        )

        // then
        expr.shouldBeTypeOf<FnCall>() should { call ->
            call.function.shouldBeTypeOf<VariableAccess>() should { fn ->
                fn.name shouldBe "method"
                fn.moduleName shouldBe ctx.currentModule
                fn.packageName shouldBe ctx.currentPackage
            }
            call.callTypeParameters shouldBe emptyList()
            call.parameters shouldHaveSize 2
            call.parameters[0].shouldBeVariable("object")
            call.parameters[1].shouldBeAtom("10", Type.intType)
        }
    }

    @Test
    fun `conversion of package function invocation`() {
        // given imported package foo/bar as pkg
        val namespace = GlobalCompilationNamespace()
        val ctx = ConversionContext(namespace)
        ctx.imports.addImport(Import("foo", "bar", "pkg", emptyList(), withinSameModule = true, null))

        // when
        val expr = convertMethodInvocation(
            ctx, ParseMethodInvocation(
                receiverName = "pkg",
                methodName = "func",
                receiver = ParseVariableRead("pkg", null),
                concreteTypeParameters = emptyList(),
                arguments = listOf(LongValue(10, null)),
                memberSection = null,
                section = null,
            )
        )

        // then
        expr.shouldBeTypeOf<FnCall>() should { call ->
            call.function.shouldBeTypeOf<VariableAccess>() should {
                it.name shouldBe "func"
                it.moduleName shouldBe "foo"
                it.packageName shouldBe "bar"
            }
            call.callTypeParameters shouldBe emptyList()
            call.parameters shouldHaveSize 1
            call.parameters[0].shouldBeAtom("10", Type.intType)
        }
    }

    @Test
    fun `conversion should find functions within the package the type was defined in`() {
        // given a type and simple function in other package
        val ctx = ConversionContext(GlobalCompilationNamespace())
        ctx.currentPackageDescriptor.typeRegistry.defineVariantType(
            testType, listOf(testType.variant!!)
        )
        ctx.namespace.getOrCreatePackage(testType.moduleName, testType.packageName).scope
            .addSymbol("method", Type.fn(Type.intType, testType), SymbolType.Local)

        ctx.namespace.getDefaultPackage().scope
            .addSymbol("object", testType, SymbolType.Local)

        // when
        val expr = convertMethodInvocation(
            ctx, ParseMethodInvocation(
                receiverName = "object",
                methodName = "method",
                receiver = ParseVariableRead("object", null),
                concreteTypeParameters = emptyList(),
                arguments = emptyList(),
                memberSection = null,
                section = null,
            )
        )

        // then
        expr.shouldBeTypeOf<FnCall>() should { call ->
            call.function.shouldBeTypeOf<VariableAccess>() should {
                it.moduleName shouldBe testType.moduleName
                it.packageName shouldBe testType.packageName
                it.name shouldBe "method"
            }
        }
    }
}

private val testType = prepareTestVariant()

private fun prepareTestVariant(): VariantType {
    val variant = VariantType.Variant(
        public = true,
        variantName = "Test",
        fields = emptyList()
    )
    return VariantType(
        moduleName = "foo",
        packageName = "bar",
        simpleName = "Test",
        genericTypeParameters = emptyList(),
        concreteTypeParameters = emptyMap(),
        variant = variant,
    )
}

fun Expression.shouldBeVariable(name: String) {
    this.shouldBeTypeOf<VariableAccess>().name shouldBe name
}