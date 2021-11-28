package gh.marad.chi.core.analyzer

import gh.marad.chi.ast
import gh.marad.chi.asts
import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.f32
import gh.marad.chi.core.Type.Companion.f64
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class AssignmentTypeCheckingSpec : FunSpec() {
    init {
        test("should check that type of the variable matches type of the expression") {
            val scope = CompilationScope()
            scope.addLocalName("x", ast("5"))
            checkTypes(ast("x = 10", scope)).shouldBeEmpty()
            checkTypes(ast("x = fn() {}", scope)).shouldHaveSingleElement(
                TypeMismatch(i32, Type.fn(unit), Location(1, 2))
            )
        }

        test("should check that type of external variable matches type of the expression") {
            val scope = CompilationScope()
            scope.defineExternalName("x", i32)
            checkTypes(ast("x = 10", scope)).shouldBeEmpty()
            checkTypes(ast("x = fn() {}", scope)).shouldHaveSingleElement(
                TypeMismatch(i32, Type.fn(unit), Location(1, 2))
            )
        }
    }
}

class NameDeclarationTypeCheckingSpec : FunSpec() {
    init {

        test("should return nothing for simple atom and variable read") {
            val scope = CompilationScope()
            scope.addLocalName("x", ast("val x = fn() {}"))
            checkTypes(ast("5", scope)).shouldBeEmpty()
            checkTypes(ast("x", scope)).shouldBeEmpty()
        }

        test("should check if types match in name declaration with type definition") {
            checkTypes(ast("val x: () -> i32 = 5"))
                .shouldHaveSingleElement(
                    TypeMismatch(Type.fn(i32), i32, Location(1, 19))
                )
        }

        test("should pass valid name declarations") {
            checkTypes(ast("val x: i32 = 5")).shouldBeEmpty()
            checkTypes(ast("val x = 5")).shouldBeEmpty()
        }
    }
}

class BlockExpressionTypeCheckingSpec : FunSpec() {
    init {
        test("should check contained expressions") {
            val block = Block(asts("""
                val x: () -> i32 = 10
                fn(): i32 {}
            """.trimIndent()), null)

            val errors = analyze(block)
            errors.shouldHaveSize(2)
            errors.shouldContain(TypeMismatch(Type.fn(i32), i32, Location(1, 19)))
            errors.shouldContain(MissingReturnValue(i32, Location(2, 10)))
        }
    }
}

class FnTypeCheckingSpec : FunSpec() {
    init {
        test("should not return errors on valid function definition") {
            analyze(ast("fn(x: i32): i32 { x }")).shouldBeEmpty()
        }

        test("should check for missing return value only if function expects the return type") {
            analyze(ast("fn() {}"))
                .shouldBeEmpty()
            analyze(ast("fn(): i32 {}"))
                .shouldHaveSingleElement(MissingReturnValue(i32, Location(1, 10)))
        }

        test("should check that block return type matches what function expects") {
            analyze(ast("fn(): i32 { fn() {} }"))
                .shouldHaveSingleElement(TypeMismatch(i32, Type.fn(unit), Location(1, 12)))

            // should point to '{' of the block when it's empty instead of last expression
            analyze(ast("fn(): i32 {}"))
                .shouldHaveSingleElement(MissingReturnValue(i32, Location(1, 10)))
        }

        test("should also check types for expressions in function body") {
            analyze(ast("""
                fn(x: i32): i32 {
                    val i: i32 = fn() {}
                    x
                }
            """.trimIndent()))
                .shouldHaveSingleElement(TypeMismatch(i32, Type.fn(unit), Location(2, 17)))
        }
    }
}

class FnCallTypeCheckingSpec : FunSpec() {
    init {
        val scope = CompilationScope()
        scope.defineExternalName("x", i32)
        scope.addLocalName("test", ast("fn(a: i32, b: () -> unit): i32 { a }"))

        test("should check that parameter argument types match") {
            analyze(ast("test(10, fn(){})", scope)).shouldBeEmpty()
            analyze(ast("test(10, 20)", scope))
                .shouldHaveSingleElement(TypeMismatch(Type.fn(unit), i32, Location(1, 9)))
        }

        test("should check function arity") {
            analyze(ast("test(1)", scope))
                .shouldHaveSingleElement(FunctionArityError("test", 2, 1, Location(1, 0)))
        }

        test("should check that only functions are called") {
            analyze(ast("x()", scope)) shouldHaveSingleElement NotAFunction("x", Location(1, 0))
        }
    }
}

class IfElseTypeCheckingSpec : FunSpec() {
    init {
        test("should check that if and else branches have the same type") {
            analyze(ast("if(true) { 2 }")).shouldBeEmpty()
            analyze(ast("if(true) { 2 } else { 3 }")).shouldBeEmpty()
            analyze(ast("if(true) { 2 } else { fn() {} }"))
                .shouldHaveSingleElement(
                    IfElseBranchesTypeMismatch(i32, Type.fn(unit))
                )
        }

        test("conditions should be boolean type") {
            analyze(ast("if (1) { 2 }")) shouldHaveSingleElement
                    TypeMismatch(
                        expected = bool,
                        actual = i32,
                        Location(1, 4)
                    )
        }
    }
}

class PrefixOpSpec : FunSpec({
    test("should expect boolean type for '!' operator") {
        analyze(ast("!true")) shouldHaveSize 0
        analyze(ast("!1")) shouldHaveSingleElement
                TypeMismatch(
                    expected = bool,
                    actual = i32,
                    Location(1, 0)
                )
    }
})

class CastSpec : FunSpec({
    test("should not allow casting to bool type") {
        analyze(ast("5 as bool")) shouldHaveSingleElement
                TypeMismatch(
                    expected = bool,
                    actual = i32,
                    Location(1, 0)
                )
    }
})