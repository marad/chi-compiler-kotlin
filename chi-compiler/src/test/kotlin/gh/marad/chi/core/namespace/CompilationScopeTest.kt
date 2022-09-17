package gh.marad.chi.core.namespace

import gh.marad.chi.core.FnType
import gh.marad.chi.core.OverloadedFnType
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.string
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class CompilationScopeTest {
    @Test
    fun `functions with the same parameter types and different return types will not create overloaded fn`() {
        // given
        val scope = CompilationScope(ScopeType.Package)
        val paramTypes = arrayOf(intType, string)
        // when
        scope.addSymbol("func", fn(returnType = unit, *paramTypes), SymbolType.Local)
        scope.addSymbol("func", fn(returnType = intType, *paramTypes), SymbolType.Local)
        // then
        scope.getSymbolType("func")
            .shouldNotBeNull()
            .shouldBeTypeOf<FnType>()
    }

    @Test
    fun `test symbol type determining`() {
        val intStringIntFn = fn(returnType = intType, intType, string)
        val intStringUnitFn = fn(returnType = unit, intType, string)
        val stringIntFn = fn(returnType = intType, string)

        // for different types - the provided one wins
        determineSymbolType(intType, string) shouldBe string
        determineSymbolType(intType, fn(unit)) shouldBe fn(unit)
        determineSymbolType(fn(unit), intType) shouldBe intType
        // for function types
        // identical type should stay the same
        determineSymbolType(intStringIntFn, intStringIntFn) shouldBe intStringIntFn
        // type with different return type should win but not overload
        determineSymbolType(intStringIntFn, intStringUnitFn) shouldBe intStringUnitFn
        // types with different parameters should create an overloaded type
        determineSymbolType(intStringIntFn, stringIntFn) shouldBe OverloadedFnType(setOf(intStringIntFn, stringIntFn))
        // already overloaded type should simply add another function type
        determineSymbolType(OverloadedFnType(setOf(intStringIntFn, stringIntFn)), fn(unit)) shouldBe
                OverloadedFnType(setOf(intStringIntFn, stringIntFn, fn(unit)))
    }
}