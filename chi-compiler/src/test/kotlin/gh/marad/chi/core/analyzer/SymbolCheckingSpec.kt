package gh.marad.chi.core.analyzer

import gh.marad.chi.core.CompilationDefaults.defaultModule
import gh.marad.chi.core.CompilationDefaults.defaultPacakge
import gh.marad.chi.core.FnCall
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.SymbolScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize

@Suppress("unused")
class SymbolCheckingSpec : FunSpec({
    test("should check that variable in VariableAccess is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = VariableAccess(defaultModule, defaultPacakge, emptyCompilationScope, "x", null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("x", null)
    }

    test("should not emit error message if the variable is defined in scope") {
        val scope = CompilationScope()
        scope.addSymbol("x", Type.intType, SymbolScope.Local)
        val expr = VariableAccess(defaultModule, defaultPacakge, scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should check that function in FnCall is defined in scope") {
        val emptyCompilationScope = CompilationScope()
        val expr = FnCall(
            emptyCompilationScope,
            "funcName",
            VariableAccess(defaultModule, defaultPacakge, emptyCompilationScope, "funcName", null),
            emptyList(),
            emptyList(),
            null
        )

        val result = analyze(expr)

        result shouldHaveSingleElement NotAFunction(null)
    }

    test("should not emit error message if function is defined in scope") {
        val scope = CompilationScope()
        scope.addSymbol("funcName", Type.fn(Type.unit), SymbolScope.Local)
        val expr = FnCall(
            scope,
            "funcName",
            VariableAccess(defaultModule, defaultPacakge, scope, "funcName", null),
            emptyList(),
            emptyList(),
            null
        )

        val result = analyze(expr)

        result shouldHaveSize 0
    }
})