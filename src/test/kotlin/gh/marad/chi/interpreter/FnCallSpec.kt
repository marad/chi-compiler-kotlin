package gh.marad.chi.interpreter

import gh.marad.chi.core.Atom
import gh.marad.chi.core.Type
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize
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
    }
}
