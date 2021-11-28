package gh.marad.chi.core.analyzer

import gh.marad.chi.ast
import gh.marad.chi.asts
import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TypeInferenceSpec : FunSpec() {
    init {
        test("should read type from assignment's value") {
            inferType(ast("x = 5")).shouldBe(i32)
        }

        test("should read type for simple atom") {
            inferType(ast("5")).shouldBe(i32)
        }

        test("should read type from definition") {
            inferType(ast("val x: i32 = 0")).shouldBe(i32)
        }

        test("should infer type from declaration expression") {
            inferType(ast("val x = 0")).shouldBe(i32)
        }

        test("should infer type form accessing variable in scope")  {
            val scope = CompilationScope()
            scope.addSymbol("x", i32)
            inferType(ast("x", scope)).shouldBe(i32)
        }

        test("block type is depends on it's last expression") {
            val block = Block(asts("""
                fn() {}
                14
            """.trimIndent()), null)
            inferType(block).shouldBe(i32)
        }

        test("empty block is of type 'unit'") {
            inferType(Block(emptyList(), null)).shouldBe(unit)
        }

        test("functions should have '() -> unit' type") {
            inferType(ast("fn() {}")).shouldBe(Type.fn(unit))
        }

        test("should take parameters into account") {
            val fn = ast("fn(x: i32): i32 { x }") as Fn
            inferType(fn.block.body.first())
                .shouldBe(i32)
        }

        test("function calls should have it's returned value type") {
            val scope = CompilationScope()
            scope.addSymbol("main", Type.fn(unit))
            scope.addSymbol("foo", Type.fn(i32))

            inferType(ast("main()", scope)).shouldBe(unit)
            inferType(ast("foo()", scope)).shouldBe(i32)
        }

        test("should throw exception when scope does not contain required variable") {
            shouldThrow<MissingVariable> { inferType(ast("notExisting")) }
            shouldThrow<MissingVariable> { inferType(ast("notExisting()"))}
        }

        test("should infer type for function call when expression evaluates to function") {
            val scope = CompilationScope()
            scope.addSymbol("test", Type.fn(i32, i32, i32))
            scope.addSymbol("foo", Type.fn(i32, i32, i32))

            inferType(ast("foo()", scope))
        }

        test("should throw exception when trying to invoke function") {
            // TODO - with type hierarchy if-else expression should have the "broader" type
            val scope = CompilationScope()
            scope.addSymbol("x", i32)

            shouldThrow<FunctionExpected> { inferType(ast("x()", scope)) }
        }

        test("should infer type for if-else expression") {
            inferType(ast("if(1) { 2 }")).shouldBe(unit)
            inferType(ast("if(1) { 2 } else { 3 }")).shouldBe(i32)
        }
    }
}