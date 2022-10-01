package gh.marad.chi.core.analyzer

import gh.marad.chi.compile
import gh.marad.chi.core.CompilationDefaults.defaultModule
import gh.marad.chi.core.CompilationDefaults.defaultPacakge
import gh.marad.chi.core.FnCall
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class SymbolCheckingSpec : FunSpec({
    test("should check that variable in VariableAccess is defined in scope") {
        val emptyCompilationScope = CompilationScope(ScopeType.Package)
        val expr = VariableAccess(defaultModule, defaultPacakge, emptyCompilationScope, "x", isModuleLocal = true, null)

        val result = analyze(expr)

        result shouldHaveSingleElement UnrecognizedName("x", null)
    }

    test("should not emit error message if the variable is defined in scope") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("x", Type.intType, SymbolType.Local)
        val expr = VariableAccess(defaultModule, defaultPacakge, scope, "x", isModuleLocal = true, null)

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should check that function in FnCall is defined in scope") {
        val emptyCompilationScope = CompilationScope(ScopeType.Package)
        val expr = FnCall(
            VariableAccess(
                defaultModule,
                defaultPacakge,
                emptyCompilationScope,
                "funcName",
                isModuleLocal = true,
                null
            ),
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
            VariableAccess(defaultModule, defaultPacakge, scope, "funcName", isModuleLocal = true, null),
            emptyList(),
            emptyList(),
            null
        )

        val result = analyze(expr)

        result shouldHaveSize 0
    }

    test("should not allow using non-public variant constructors from other module") {
        val namespace = GlobalCompilationNamespace()
        val typeDef = """
            package foo/bar
            data Foo = Foo(i: int) | pub Bar(i: int)
        """.trimIndent()
        compile(typeDef, namespace)

        val import = """
            import foo/bar { Foo }
            val bar = Bar(10)
            val foo = Foo(20)
        """.trimIndent()

        val ast = compile(import, namespace, ignoreCompilationErrors = true)

        ast[1].shouldBeTypeOf<NameDeclaration>()
            .name shouldBe "bar"
        analyze(ast[1]) shouldHaveSize 0

        ast[2].shouldBeTypeOf<NameDeclaration>()
            .name shouldBe "foo"
        analyze(ast[2]) should {
            it shouldHaveSize 1
            it[0].shouldBeTypeOf<CannotAccessInternalName>()
                .name shouldBe "Foo"
        }
    }
})