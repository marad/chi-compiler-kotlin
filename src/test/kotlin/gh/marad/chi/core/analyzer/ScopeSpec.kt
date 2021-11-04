package gh.marad.chi.core.analyzer

import gh.marad.chi.core.FnParam
import gh.marad.chi.core.Type
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ScopeSpec : FunSpec() {
    init {
        test("scope should find functions and variables on top level expressions") {
            // given
            val expressions = parse(
                tokenize("""
                val x = 5
                val main = fn() {}
            """.trimIndent())
            )

            // when
            val scope = Scope.fromExpressions(expressions)

            // then
            scope.findVariable("x", location = null).shouldNotBeNull()
            scope.findFunction("main", location = null).shouldNotBeNull()
        }

        test("function scope should also have parameter types") {
            // given
            val scope = Scope()

            // when
            scope.defineFunctionParams(listOf(
                FnParam("x", Type.i32, null)
            ))

            // then
            scope.getFunctionParamType("x").shouldBe(Type.i32)
        }

        test("should search functions and variables in parent scope") {
            // given
            val expressions = parse(
                tokenize("""
                val x = 5
                val main = fn() {}
            """.trimIndent())
            )
            val parentScope = Scope.fromExpressions(expressions)

            // when
            val scope = Scope(parentScope)

            // then
            scope.findFunction("main", null).shouldNotBeNull()
            scope.findVariable("x", null).shouldNotBeNull()

        }

        test("should throw error when function or variable is not found") {
            val emptyScope = Scope()
            shouldThrow<RuntimeException> { emptyScope.findFunction("nonExisting", null) }
            shouldThrow<RuntimeException> { emptyScope.findVariable("nonExisting", null) }
        }
    }
}