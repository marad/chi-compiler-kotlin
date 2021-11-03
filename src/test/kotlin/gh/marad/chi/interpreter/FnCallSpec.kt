package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FnCallSpec : FunSpec() {

    private fun Scope.eval(code: String) =
        parse(tokenize(code)).map { eval(it) }.last()

    init {
        test("body should have access to outer scope") {
            // given
            val scope = Scope()
            scope.eval("val x = 5")
            scope.eval("val foo = fn() { x }")

            // when
            val result = scope.eval("foo()")

            // then
            result.shouldBe(Atom("5", Type.i32))
        }

        test("function should be able to use arguments") {
            // given
            val scope = Scope()
            scope.eval("val x = 5")
            scope.eval("val foo = fn(bar: i32) { bar }")

            // when
            val result = scope.eval("foo(10)")

            // then
            result.shouldBe(Atom("10", Type.i32))
        }

        test("arguments should hide parent scope variables with the same name") {
            // given
            val scope = Scope()
            scope.eval("val x = 5")
            scope.eval("val foo = fn(x: i32) { x }")

            // when
            val result = scope.eval("foo(10)")

            // then
            result.shouldBe(Atom("10", Type.i32))
        }

        test("using native functions") {
            // given
            val scope = Scope()
            scope.registerNativeFunction("add") { _, args ->
                val a = (args[0] as Atom).value.toInt()
                val b = (args[1] as Atom).value.toInt()
                Atom((a+b).toString(), Type.i32)
            }

            // expect
            scope.eval("add(5, 3)")
                .shouldBe(
                    Atom("8", Type.i32)
                )

        }
    }
}
