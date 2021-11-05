package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize

fun asts(code: String): List<Expression> = parse(tokenize(code))
fun ast(code: String): Expression = asts(code).last()

class NameDeclarationTypeCheckingSpec : FunSpec() {
    init {

        test("should return nothing for simple atom and variable read") {
            val scope = Scope.fromExpressions(asts("val x = fn() {}"))
            checkTypes(scope, ast("5")).shouldBeEmpty()
            checkTypes(scope, ast("x")).shouldBeEmpty()
        }

        test("should check if types match in name declaration with type definition") {
            checkTypes(Scope(), ast("val x: () -> i32 = 5"))
                .shouldHaveSingleElement(
                    TypeMismatch(Type.fn(i32), i32, Location(0, 19))
                )
        }

        test("should pass valid name declarations") {
            checkTypes(Scope(), ast("val x: i32 = 5")).shouldBeEmpty()
            checkTypes(Scope(), ast("val x = 5")).shouldBeEmpty()
        }
    }
}

class BlockExpressionTypeCheckingSpec : FunSpec() {
    init {
        test("should check contained expressions") {
            val block = BlockExpression(asts("""
                val x: () -> i32 = 10
                fn(): i32 {}
            """.trimIndent()), null)

            val errors = checkTypes(Scope(), block)
            errors.shouldHaveSize(2)
            errors.shouldContain(TypeMismatch(Type.fn(i32), i32, Location(0, 19)))
            errors.shouldContain(MissingReturnValue(i32, Location(1, 10)))
        }
    }
}

class FnTypeCheckingSpec : FunSpec() {
    init {
        test("should not return errors on valid function definition") {
            checkTypes(Scope(), ast("fn(x: i32): i32 { x }")).shouldBeEmpty()
        }

        test("should check for missing return value only if function expects the return type") {
            checkTypes(Scope(), ast("fn() {}"))
                .shouldBeEmpty()
            checkTypes(Scope(), ast("fn(): i32 {}"))
                .shouldHaveSingleElement(MissingReturnValue(i32, Location(0, 10)))
        }

        test("should check that block return type matches what function expects") {
            checkTypes(Scope(), ast("fn(): i32 { fn() {} }"))
                .shouldHaveSingleElement(TypeMismatch(i32, Type.fn(unit), Location(0, 12)))

            // should point to '{' of the block when it's empty instead of last expression
            checkTypes(Scope(), ast("fn(): i32 {}"))
                .shouldHaveSingleElement(MissingReturnValue(i32, Location(0, 10)))
        }

        test("should also check types for expressions in function body") {
            checkTypes(Scope(), ast("""
                fn(x: i32): i32 {
                    val i: i32 = fn() {}
                    x
                }
            """.trimIndent()))
                .shouldHaveSingleElement(TypeMismatch(i32, Type.fn(unit), Location(1, 17)))
        }
    }
}

class FnCallTypeCheckingSpec : FunSpec() {
    init {

        val scope = Scope.fromExpressions(asts("val test = fn(a: i32, b: () -> unit): i32 { a }"))

        test("should check that parameter argument types match") {
            checkTypes(scope, ast("test(10, fn(){})")).shouldBeEmpty()
            checkTypes(scope, ast("test(10, 20)"))
                .shouldHaveSingleElement(TypeMismatch(Type.fn(unit), i32, Location(0, 9)))
        }

        test("should check function arity") {
            checkTypes(scope, ast("test(1)"))
                .shouldHaveSingleElement(FunctionArityError("test", 2, 1, Location(0, 0)))
        }
    }
}