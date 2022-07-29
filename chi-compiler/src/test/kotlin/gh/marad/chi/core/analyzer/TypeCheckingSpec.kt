package gh.marad.chi.core.analyzer

import gh.marad.chi.ast
import gh.marad.chi.compileWithScope
import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.floatType
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class AssignmentTypeCheckingSpec : FunSpec() {
    init {
        test("should check that type of the variable matches type of the expression") {
            val scope = CompilationScope()
            scope.addSymbol("x", intType, SymbolScope.Local)
            analyze(ast("x = 10", scope)).shouldBeEmpty()
            analyze(ast("x = fn() {}", scope, ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }
        }
    }
}

class NameDeclarationTypeCheckingSpec : FunSpec() {
    init {

        test("should return nothing for simple atom and variable read") {
            val scope = CompilationScope()
            scope.addSymbol("x", Type.fn(unit), SymbolScope.Local)
            analyze(ast("5", scope, ignoreCompilationErrors = true)).shouldBeEmpty()
            analyze(ast("x", scope, ignoreCompilationErrors = true)).shouldBeEmpty()
        }

        test("should check if types match in name declaration with type definition") {
            analyze(ast("val x: () -> int = 5", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe Type.fn(intType)
                    error.actual shouldBe intType
                }
            }
        }

        test("should pass valid name declarations") {
            analyze(ast("val x: int = 5", ignoreCompilationErrors = true)).shouldBeEmpty()
            analyze(ast("val x = 5", ignoreCompilationErrors = true)).shouldBeEmpty()
        }
    }
}

class BlockExpressionTypeCheckingSpec : FunSpec() {
    init {
        test("should check contained expressions") {
            val block = Block(compileWithScope("""
                val x: () -> int = 10
                fn(): int {}
            """.trimIndent(), ignoreCompilationErrors = true), null, )

            val errors = analyze(block)
            errors.shouldHaveSize(2)
            errors.should {
                errors[0].shouldBeTypeOf<TypeMismatch>().should {error ->
                    error.expected shouldBe Type.fn(intType)
                    error.actual shouldBe intType
                }
                errors[1].shouldBeTypeOf<MissingReturnValue>().should {error ->
                    error.expectedType shouldBe intType
                }
            }
        }
    }
}

class FnTypeCheckingSpec : FunSpec() {
    init {
        test("should not return errors on valid function definition") {
            analyze(ast("fn(x: int): int { x }", ignoreCompilationErrors = true)).shouldBeEmpty()
        }

        test("should check for missing return value only if function expects the return type") {
            analyze(ast("fn() {}"))
                .shouldBeEmpty()
            analyze(ast("fn(): int {}", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<MissingReturnValue>().should { error ->
                    error.expectedType shouldBe intType
                }
            }
        }

        test("should check that block return type matches what function expects") {
            analyze(ast("fn(): int { fn() {} }", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should {error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }

            // should point to '{' of the block when it's empty instead of last expression
            analyze(ast("fn(): int {}", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<MissingReturnValue>().should { error ->
                    error.expectedType shouldBe intType
                }
            }
        }

        test("should also check types for expressions in function body") {
            analyze(ast("""
                fn(x: int): int {
                    val i: int = fn() {}
                    x
                }
            """.trimIndent(), ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should {error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }
        }
    }
}

class FnCallTypeCheckingSpec : FunSpec() {
    init {
        val scope = CompilationScope()
        scope.addSymbol("x", intType, SymbolScope.Local)
        scope.addSymbol("test", Type.fn(intType, intType, Type.fn(unit)), SymbolScope.Local)

        test("should check that parameter argument types match") {
            analyze(ast("test(10, fn(){})", scope)).shouldBeEmpty()
            analyze(ast("test(10, 20)", scope, ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should {error ->
                    error.expected shouldBe Type.fn(unit)
                    error.actual shouldBe intType
                }
            }
        }

        test("should check function arity") {
            analyze(ast("test(1)", scope, ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<FunctionArityError>().should { error ->
                    error.expectedCount shouldBe 2
                    error.actualCount shouldBe 1
                }
            }
        }

        test("should check that only functions are called") {
            analyze(ast("x()", scope, ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<NotAFunction>()
            }
        }

        test("should check that proper overloaded function exists") {
            val localScope = CompilationScope()
            localScope.addSymbol("test", Type.fn(intType, intType), SymbolScope.Local)
            localScope.addSymbol("test", Type.fn(intType, floatType), SymbolScope.Local)

            analyze(ast("test(2)", localScope)).shouldBeEmpty()
            analyze(ast("test(2 as unit)", localScope, ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<NoCandidatesForFunction>().should { error ->
                    error.argumentTypes shouldBe listOf(unit)
                }
            }
        }
    }
}

class IfElseTypeCheckingSpec : FunSpec() {
    init {
        test("should check that if and else branches have the same type") {
            analyze(ast("if(true) { 2 }", ignoreCompilationErrors = true)).shouldBeEmpty()
            analyze(ast("if(true) { 2 } else { 3 }", ignoreCompilationErrors = true)).shouldBeEmpty()
            analyze(ast("if(true) { 2 } else { fn() {} }", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<IfElseBranchesTypeMismatch>().should { error ->
                    error.thenBranchType shouldBe intType
                    error.elseBranchType shouldBe Type.fn(unit)
                }
            }
        }

        test("conditions should be boolean type") {
            analyze(ast("if (1) { 2 }", ignoreCompilationErrors = true)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe bool
                    error.actual shouldBe intType
                }
            }
        }
    }
}

class PrefixOpSpec : FunSpec({
    test("should expect boolean type for '!' operator") {
        analyze(ast("!true", ignoreCompilationErrors = true)) shouldHaveSize 0
        analyze(ast("!1", ignoreCompilationErrors = true)).should {
            it.shouldHaveSize(1)
            it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                error.expected shouldBe bool
                error.actual shouldBe intType
            }
        }
    }
})

class CastSpec : FunSpec({
    test("should not allow casting to bool type") {
        analyze(ast("5 as bool", ignoreCompilationErrors = true)).should {
            it.shouldHaveSize(1)
            it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                error.expected shouldBe bool
                error.actual shouldBe intType
            }
        }
    }
})

class WhileLoopSpec : FunSpec({
    test("condition should have boolean type") {
        analyze(ast("while(true) {}", ignoreCompilationErrors = true)) shouldHaveSize 0
        analyze(ast("while(1) {}", ignoreCompilationErrors = true)).should {
            it.shouldHaveSize(1)
            it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                error.expected shouldBe bool
                error.actual shouldBe intType
            }
        }
    }
})