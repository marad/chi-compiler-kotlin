package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.Scope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FnCallSpec : FunSpec() {

    private fun Interpreter.eval(scope: Scope<Expression>, code: String) =
        parse(tokenize(code)).map { eval(scope, it) }.last()

    init {
        test("body should have access to outer scope") {
            // given
            val interpreter = Interpreter()
            val scope = Scope<Expression>()
            interpreter.eval(scope, "val x = 5")
            interpreter.eval(scope, "val foo = fn() { x }")

            // when
            val result = interpreter.eval(scope, "foo()")

            // then
            result.shouldBe(Atom("5", Type.i32, Location(0, 8)))
        }

        test("function should be able to use arguments") {
            // given
            val interpreter = Interpreter()
            val scope = Scope<Expression>()
            interpreter.eval(scope, "val x = 5")
            interpreter.eval(scope, "val foo = fn(bar: i32) { bar }")

            // when
            val result = interpreter.eval(scope, "foo(10)")

            // then
            result.shouldBe(Atom("10", Type.i32, Location(0, 4)))
        }

        test("arguments should hide parent scope variables with the same name") {
            // given
            val interpreter = Interpreter()
            val scope = Scope<Expression>()
            interpreter.eval(scope, "val x = 5")
            interpreter.eval(scope, "val foo = fn(x: i32) { x }")

            // when
            val result = interpreter.eval(scope, "foo(10)")

            // then
            result.shouldBe(Atom("10", Type.i32, Location(0, 4)))
        }

        test("using native functions") {
            // given
            val interpreter = Interpreter()
            interpreter.registerNativeFunction("add") { _, args ->
                val a = (args[0] as Atom).value.toInt()
                val b = (args[1] as Atom).value.toInt()
                Atom((a+b).toString(), Type.i32, null)
            }

            // expect
            interpreter.eval(Scope(), "add(5, 3)")
                .shouldBe(
                    Atom("8", Type.i32, null)
                )

        }
    }
}
