package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TypeInferenceSpec : FunSpec() {
    init {
        fun asts(code: String): List<Expression> = parse(tokenize(code))
        fun ast(code: String): Expression = asts(code).last()
        test("should read type from assignment's value") {
            inferType(Scope(), ast("x = 5")).shouldBe(i32)
        }

        test("should read type for simple atom") {
            inferType(Scope(), ast("5")).shouldBe(i32)
        }

        test("should read type from definition") {
            inferType(Scope(), ast("val x: i32 = 0")).shouldBe(i32)
        }

        test("should infer type from declaration expression") {
            inferType(Scope(), ast("val x = 0")).shouldBe(i32)
        }

        test("should infer type form accessing variable in scope")  {
            val scope = Scope.fromExpressions(asts("val x = 10"))
            inferType(scope, ast("x")).shouldBe(i32)
        }

        test("block type is depends on it's last expression") {
            val block = BlockExpression(asts("""
                fn() {}
                14
            """.trimIndent()), null)
            inferType(Scope(), block).shouldBe(i32)
        }

        test("empty block is of type 'unit'") {
            inferType(Scope(), BlockExpression(emptyList(), null)).shouldBe(unit)
        }

        test("functions should have '() -> unit' type") {
            inferType(Scope(), ast("fn() {}")).shouldBe(Type.fn(unit))
        }

        test("function calls should have it's returned value type") {
            val scope = Scope.fromExpressions(asts("""
                    val main = fn() {}
                    val foo = fn(): i32 { 5 }
                """.trimIndent()))
            inferType(scope, ast("main()")).shouldBe(unit)
            inferType(scope, ast("foo()")).shouldBe(i32)
        }

        test("function calls should use external names") {
            val scope = Scope()
            scope.defineExternalName("ext", i32)
            scope.defineExternalName("extFn", Type.fn(unit, i32))

            inferType(scope, ast("ext")).shouldBe(i32)
            inferType(scope, ast("extFn")).shouldBe(Type.fn(unit, i32))
            inferType(scope, ast("extFn()")).shouldBe(unit)
        }

        test("locally defined names should shadow external ones") {
            val scope = Scope()
            scope.defineExternalName("foo", Type.fn(i32))
            inferType(scope, ast("foo()")).shouldBe(i32)
            inferType(scope, ast("foo")).shouldBe(Type.fn(i32))

            scope.defineVariable("foo", ast("fn(x: i32) {}"))

            inferType(scope, ast("foo()")).shouldBe(unit)
            inferType(scope, ast("foo")).shouldBe(Type.fn(returnType = unit, i32))
        }

        test("should throw exception when scope does not contain required variable") {
            val emptyScope = Scope()

            shouldThrow<MissingVariable> { inferType(emptyScope, ast("notExisting")) }
            shouldThrow<MissingVariable> { inferType(emptyScope, ast("notExisting()"))}
        }

        test("should infer type for function call when expression evaluates to function") {
            val scope = Scope.fromExpressions(asts("""
                val test = fn(x: i32, y: i32): i32 { y }
                val foo = test
            """.trimIndent()))

            inferType(scope, ast("foo()"))
        }

        test("should throw exception when trying to invoke function") {
            // TODO - with type hierarchy if-else expression should have the "broader" type
            val scope = Scope.fromExpressions(asts("""
                val x = 5
            """.trimIndent()))

            shouldThrow<FunctionExpected> { inferType(scope, ast("x()")) }
        }

        test("should infer type for if-else expression") {
            inferType(Scope(), ast("if(1) { 2 }")).shouldBe(i32)
            inferType(Scope(), ast("if(1) { 2 } else { 3 }")).shouldBe(i32)
        }

    }
}