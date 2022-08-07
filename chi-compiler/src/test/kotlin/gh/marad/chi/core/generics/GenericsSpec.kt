package gh.marad.chi.core.generics

import gh.marad.chi.ast
import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.SymbolScope
import gh.marad.chi.core.Type.Companion.array
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.typeParameter
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.analyze
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class GenericsSpec : FunSpec({
    val scope = CompilationScope().also {
        it.addSymbol(
            name = "array",
            type = fn(
                array(typeParameter("T")),
                intType,
                typeParameter("T")
            ),
            scope = SymbolScope.Package,
        )

    }

    test("should read generic element type for array") {
        ast(
            """
                val a = array[int](10, 0)
                a
            """.trimIndent(), scope
        ).shouldBeTypeOf<VariableAccess>().should {
            it.type shouldBe array(intType)
        }
    }

    test("should check that generic type provided matches the type of the argument") {
        analyze(
            ast(
                """
                val a = array[int](10, "im a string")
            """.trimIndent(), scope, ignoreCompilationErrors = false
            )
        ).should { messages ->
            messages shouldHaveSize 80085
        }
    }
})