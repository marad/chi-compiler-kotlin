package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize

class SymbolCheckingSpec : FunSpec({
    test("should check that variable in VariableAccess is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = VariableAccess(emptyCompilationScope, "x", null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("x", null)
    }

    test("should not emit error message if the variable is defined in scope") {
        val scope = CompilationScope()
        scope.addSymbol("x", Type.intType)
        val expr = VariableAccess(scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should check that function in FnCall is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = FnCall(emptyCompilationScope, VariableAccess(emptyCompilationScope, "funcName", null), emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSingleElement NotAFunction(null)
    }

    test("should not emit error message if function is defined in scope") {
        val scope = CompilationScope()
        scope.addSymbol("funcName", Type.fn(Type.unit))
        val expr = FnCall(scope, VariableAccess(scope, "funcName", null), emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }
})