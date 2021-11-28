package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldHave

class SymbolCheckingSpec : FunSpec({
    test("should check that variable in VariableAccess is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = VariableAccess(emptyCompilationScope, "x", null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("x", null)
    }

    test("should not emit error message if the variable is defined in scope") {
        val scope = CompilationScope()
        scope.addLocalName("x", Atom.i32(1, null))
        val expr = VariableAccess(scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should not emit error message if symbol is defined as external") {
        val scope = CompilationScope()
        scope.defineExternalName("x", Type.i32)
        val expr = VariableAccess(scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should not emit error message if symbol is defined as parameter") {
        val scope = CompilationScope()
        scope.addParameter("x", Type.i32)
        val expr = VariableAccess(scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should check that function in FnCall is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = FnCall(emptyCompilationScope, "funcName", emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("funcName", null)
    }

    test("should not emit error message if function is defined in scope") {
        val scope = CompilationScope()
        scope.addLocalName("funcName",
            Fn(CompilationScope(parent = scope), emptyList(), Type.unit, Block(emptyList(), null), null))
        val expr = FnCall(scope, "funcName", emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should not emit error message if function is defined as external") {
        val scope = CompilationScope()
        scope.defineExternalName("funcName", Type.fn(Type.unit))
        val expr = FnCall(scope, "funcName", emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should not emit error message if function is defined as parameter") {
        val scope = CompilationScope()
        scope.addParameter("funcName", Type.fn(Type.unit))
        val expr = FnCall(scope, "funcName", emptyList(), null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }
})