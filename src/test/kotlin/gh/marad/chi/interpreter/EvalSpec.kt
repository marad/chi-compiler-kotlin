package gh.marad.chi.interpreter

import gh.marad.chi.ast
import gh.marad.chi.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EvalSpec : FunSpec() {

    init {
        test("should eval simple atom to itself") {
            // given
            val interpreter = Interpreter()

            // expect
            interpreter.eval(intAtom)
                .shouldBe(intAtom)
        }

        test("evaling variable read should return value from scope") {
            // given
            val interpreter = Interpreter()
            interpreter.topLevelExecutionScope.define("foo", intAtom)

            // expect
            interpreter.eval(VariableAccess(NewScope(), "foo", null))
                .shouldBe(intAtom)
        }

        test("evaluating name declaration should update scope") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval(NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null))

            // then
            result.shouldBe(intAtom)
            interpreter.topLevelExecutionScope.get("x").shouldBe(intAtom)
        }

        test("function definition should evaluate to itself") {
            // given
            val interpreter = Interpreter()
            val emptyFn = Fn(
                parameters = emptyList(),
                returnType = Type.i32,
                block = Block(emptyList(), null),
                location = null,
                fnScope = NewScope()
            )

            // expect
            interpreter.eval(emptyFn)
                .shouldBe(emptyFn)
        }

        test("block expression should return last expression value") {
            // given
            val interpreter = Interpreter()
            val lastAtom = Atom("10", Type.i32, null)

            // when
            val result = interpreter.eval(Block(listOf(
                NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null),
                NameDeclaration("y", lastAtom, immutable = false, expectedType = Type.i32, null)
            ), null))

            // then
            result.shouldBe(lastAtom)
        }

        test("block expression should evaluate all expressions") {
            // given
            val interpreter = Interpreter()
            val lastAtom = Atom("10", Type.i32, null)

            // when
            interpreter.eval(Block(listOf(
                NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null),
                NameDeclaration("y", lastAtom, immutable = false, expectedType = Type.i32, null)
            ), null))

            // then
            interpreter.topLevelExecutionScope.get("x").shouldBe(intAtom)
            interpreter.topLevelExecutionScope.get("y").shouldBe(lastAtom)
        }

        test("empty block expression returns unit") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval(Block(emptyList(), null))

            // then
            result.shouldBe(Atom("()", Type.unit, null))
        }

        test("calling function should evaluate its body") {
            // given
            val interpreter = Interpreter()

            interpreter.eval(ast("""
                val foo = fn(a: i32): i32 { 
                    val x = 5
                    var y = 10
                }
            """.trimIndent()))

            // when
            val result = interpreter.eval(FnCall(NewScope(), "foo", listOf(Atom("1", Type.i32, null)), null))

            // then result is the value of last expression
            result.shouldBe(Atom("10", Type.i32, Location(2, 12)))

            // and parent scope should not change
            interpreter.topLevelExecutionScope.get("x").shouldBeNull()
            interpreter.topLevelExecutionScope.get("y").shouldBeNull()
        }

    }
}

private val intAtom = Atom("5", Type.i32, null)