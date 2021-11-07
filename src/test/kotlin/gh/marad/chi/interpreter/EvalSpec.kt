package gh.marad.chi.interpreter

import gh.marad.chi.actionast.Atom
import gh.marad.chi.actionast.Block
import gh.marad.chi.actionast.Fn
import gh.marad.chi.core.Type
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EvalSpec : FunSpec() {

    init {
        test("should eval simple atom to itself") {
            // given
            val interpreter = Interpreter()
            val atom = Atom.i32(10)

            // expect
            interpreter.eval(atom)
                .shouldBe(atom)
        }

        test("evaling variable read should return value from scope") {
            // given
            val interpreter = Interpreter()
            val atom = Atom.i32(10)
            interpreter.topLevelExecutionScope.define("foo", atom)

            // expect
            interpreter.eval("foo")
                .shouldBe(atom)
        }

        test("evaluating name declaration should update scope") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval("val x: i32 = 5")

            // then
            result.shouldBe(Atom.i32(5))
            interpreter.topLevelExecutionScope.get("x").shouldBe(Atom.i32(5))
        }

        test("function definition should evaluate to itself") {
            // given
            val interpreter = Interpreter()

            // expect
            interpreter.eval("fn() {}")
                .shouldBe(Fn(
                    emptyList(),
                    returnType = Type.unit,
                    block = Block(emptyList(), Type.unit)
                ))
        }

        test("block expression should return last expression value") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval("""
                val x = 5
                var y = 10
            """.trimIndent())

            // then
            result.shouldBe(Atom.i32(10))
        }

        test("block expression should evaluate all expressions") {
            // given
            val interpreter = Interpreter()

            // when
            interpreter.eval("""
                val x = 5
                var y = 10
            """.trimIndent())

            // then
            interpreter.topLevelExecutionScope.get("x").shouldBe(Atom.i32(5))
            interpreter.topLevelExecutionScope.get("y").shouldBe(Atom.i32(10))
        }

        test("empty block expression returns unit") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval(Block(emptyList(), Type.unit))

            // then
            result.shouldBe(Atom.unit)
        }

        test("calling function should evaluate its body") {
            // given
            val interpreter = Interpreter()

            interpreter.eval("""
                val foo = fn(a: i32): i32 {
                    val x = 5
                    var y = 10
                }
            """.trimIndent())

            // when
            val result = interpreter.eval("foo(1)")

            // then result is the value of last expression
            result.shouldBe(Atom.i32(10))

            // and parent scope should not change
            interpreter.topLevelExecutionScope.get("x").shouldBeNull()
            interpreter.topLevelExecutionScope.get("y").shouldBeNull()
        }

    }
}