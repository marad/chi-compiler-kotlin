package gh.marad.chi.core.analyzer

import gh.marad.chi.core.Expression
import gh.marad.chi.core.Type
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
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
            scope.findVariable("x").shouldNotBeNull()
            scope.findVariable("main").shouldNotBeNull()
        }

        test("scope should find external name types in parent scope") {
            // given
            val parentScope = Scope<Expression>()
            val childScope = Scope(parentScope)
            parentScope.defineExternalName("foo", Type.i32)

            // expect
            childScope.getExternalNameType("foo").shouldBe(Type.i32)
        }

        test("function scope should also have parameter types") {
            // given
            val scope = Scope<Expression>()

            // when
            scope.defineExternalName("x", Type.i32)

            // then
            scope.getExternalNameType("x").shouldBe(Type.i32)
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
            scope.findVariable("main").shouldNotBeNull()
            scope.findVariable("x").shouldNotBeNull()

        }

        test("should return null when function or variable is not found") {
            val emptyScope = Scope<Expression>()
            emptyScope.findVariable("nonExisting").shouldBeNull()
        }
    }
}