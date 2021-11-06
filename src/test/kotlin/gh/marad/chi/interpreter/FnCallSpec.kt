package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.i32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FnCallSpec : FunSpec() {

    init {
        test("unit function should not return value of the last expression") {
            val interpreter = Interpreter()
            interpreter.eval("val main = fn(x: i32) { x }")
            interpreter.eval("main(10)").shouldBe(Atom.unit(null))
        }

        test("body should have access to outer scope") {
            // given
            val interpreter = Interpreter()
            interpreter.eval("val x = 5")
            interpreter.eval("val foo = fn(): i32 { x }")

            // when
            val result = interpreter.eval("foo()")

            // then
            result.shouldBe(Atom("5", i32, Location(0, 8)))
        }

        test("function should be able to use arguments") {
            // given
            val interpreter = Interpreter()
            interpreter.eval("val x = 5")
            interpreter.eval("val foo = fn(bar: i32): i32 { bar }")

            // when
            val result = interpreter.eval("foo(10)")

            // then
            result.shouldBe(Atom("10", i32, Location(0, 4)))
        }

        test("arguments should hide parent scope variables with the same name") {
            // given
            val interpreter = Interpreter()
            interpreter.eval("val x = 5")
            interpreter.eval("val foo = fn(x: i32): i32 { x }")

            // when
            val result = interpreter.eval("foo(10)")

            // then
            result.shouldBe(Atom("10", i32, Location(0, 4)))
        }

        test("using native functions") {
            // given
            val interpreter = Interpreter()
            interpreter.registerNativeFunction("add", Type.fn(i32, i32, i32)) { _, args ->
                val a = (args[0] as Atom).value.toInt()
                val b = (args[1] as Atom).value.toInt()
                Atom((a+b).toString(), i32, null)
            }

            // expect
            interpreter.eval("add(5, 3)")
                .shouldBe(
                    Atom("8", i32, null)
                )

        }
    }
}
