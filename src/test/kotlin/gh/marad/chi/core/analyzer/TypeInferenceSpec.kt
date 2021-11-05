package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TypeInferenceSpec : FunSpec() {
    init {
        fun asts(code: String): List<Expression> = parse(tokenize(code))
        fun ast(code: String): Expression = asts(code).last()
        test("should read type for simple atom") {
            inferType(Scope(), ast("5")).shouldBe(Type.i32)
        }

        test("should read type from definition") {
            inferType(Scope(), ast("val x: i32 = 0")).shouldBe(Type.i32)
        }

        test("should infer type from assigned expression expression") {
            inferType(Scope(), ast("val x = 0")).shouldBe(Type.i32)
        }

        test("should infer type form accessing variable in scope")  {
            val scope = Scope.fromExpressions(asts("val x = 10"))
            inferType(scope, ast("x")).shouldBe(Type.i32)
        }

        test("block type is depends on it's last expression") {
            val block = BlockExpression(asts("""
                fn() {}
                14
            """.trimIndent()))
            inferType(Scope(), block).shouldBe(Type.i32)
        }

        test("empty block is of type 'unit'") {
            inferType(Scope(), BlockExpression(emptyList())).shouldBe(unit)
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
            inferType(scope, ast("foo()")).shouldBe(Type.i32)
        }

        test("function calls should use external names") {
            val scope = Scope()
            scope.defineExternalName("ext", Type.i32)

            inferType(scope, ast("ext()")).shouldBe(Type.i32)
        }

        test("locally defined names should shadow external ones") {
            val scope = Scope()
            scope.defineExternalName("foo", unit)
            // TODO: tutaj jest problem - inferType() powinno zwrócić i32, ale tak na prawdę typ "foo" to jest 'fn(): i32'
            // chyba trzeba wreszcie rozkminić ten typ zmiennej dla funkcji
            scope.defineVariable("foo", ast("5"))

            inferType(scope, ast("foo()")).shouldBe(Type.i32)
        }
    }
}