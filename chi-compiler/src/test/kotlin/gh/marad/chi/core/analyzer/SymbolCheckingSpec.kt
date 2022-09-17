package gh.marad.chi.core.analyzer

import gh.marad.chi.core.CompilationDefaults.defaultModule
import gh.marad.chi.core.CompilationDefaults.defaultPacakge
import gh.marad.chi.core.FnCall
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize

@Suppress("unused")
class SymbolCheckingSpec : FunSpec({
    test("should check that variable in VariableAccess is defined in scope") {
        val emptyCompilationScope = CompilationScope(ScopeType.Package)
        val expr = VariableAccess(defaultModule, defaultPacakge, emptyCompilationScope, "x", null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("x", null)
    }

    test("should not emit error message if the variable is defined in scope") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("x", Type.intType, SymbolType.Local)
        val expr = VariableAccess(defaultModule, defaultPacakge, scope, "x", null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should check that function in FnCall is defined in scope") {
        val emptyCompilationScope = CompilationScope(ScopeType.Package)
        val expr = FnCall(
            VariableAccess(defaultModule, defaultPacakge, emptyCompilationScope, "funcName", null),
            emptyList(),
            emptyList(),
            null
        )

        val result = analyze(expr)

        result shouldContain NotAFunction(null)
    }

    test("should not emit error message if function is defined in scope") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("funcName", Type.fn(Type.unit), SymbolType.Local)
        val expr = FnCall(
            VariableAccess(defaultModule, defaultPacakge, scope, "funcName", null),
            emptyList(),
            emptyList(),
            null
        )

        val result = analyze(expr)

        result shouldHaveSize 0
    }
})