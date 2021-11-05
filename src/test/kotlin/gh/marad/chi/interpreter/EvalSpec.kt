package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.core.analyzer.Scope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EvalSpec : FunSpec() {

    init {
        test("should eval simple atom to itself") {
            // given
            val interpreter = Interpreter()

            // expect
            interpreter.eval(Scope(), intAtom)
                .shouldBe(intAtom)
        }

        test("evaling variable read should return value from scope") {
            // given
            val scope = Scope()
            scope.defineVariable("foo", intAtom)
            val interpreter = Interpreter()

            // expect
            interpreter.eval(scope, VariableAccess("foo", null))
                .shouldBe(intAtom)
        }

        test("evaluating name declaration should update scope") {
            // given
            val interpreter = Interpreter()
            val scope = Scope()

            // when
            val result = interpreter.eval(scope, NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null))

            // then
            result.shouldBe(intAtom)
            scope.findVariable("x").shouldBe(intAtom)
        }

        test("function definition should evaluate to itself") {
            // given
            val interpreter = Interpreter()
            val emptyFn = Fn(parameters = emptyList(), returnType = Type.i32, BlockExpression(emptyList(), null), null)

            // expect
            interpreter.eval(Scope(), emptyFn)
                .shouldBe(emptyFn)
        }

        test("block expression should return last expression value") {
            // given
            val interpreter = Interpreter()
            val lastAtom = Atom("10", Type.i32, null)

            // when
            val result = interpreter.eval(Scope(), BlockExpression(listOf(
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
            val scope = Scope()

            // when
            interpreter.eval(scope, BlockExpression(listOf(
                NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null),
                NameDeclaration("y", lastAtom, immutable = false, expectedType = Type.i32, null)
            ), null))

            // then
            scope.findVariable("x").shouldBe(intAtom)
            scope.findVariable("y").shouldBe(lastAtom)
        }

        test("empty block expression returns unit") {
            // given
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval(Scope(), BlockExpression(emptyList(), null))

            // then
            result.shouldBe(Atom("()", Type.unit, null))
        }

        test("calling function should evaluate its body") {
            // given
            val lastAtom = Atom("10", Type.i32, null)
            val body = BlockExpression(listOf(
                NameDeclaration("x", intAtom, immutable = true, expectedType = Type.i32, null),
                NameDeclaration("y", lastAtom, immutable = false, expectedType = Type.i32, null)
            ), null)
            val fn = Fn(
                parameters = listOf(
                    FnParam("a", Type.i32, null)
                ),
                returnType = Type.i32,
                block = body
            , null)
            val scope = Scope()
            scope.defineVariable("foo", fn)
            val interpreter = Interpreter()

            // when
            val result = interpreter.eval(scope, FnCall("foo", listOf(Atom("1", Type.i32, null)), null))

            // then result is the value of last expression
            result.shouldBe(lastAtom)

            // and parent scope should not change
            scope.findVariable("x").shouldBeNull()
            scope.findVariable("y").shouldBeNull()
        }
    }
}

private val intAtom = Atom("5", Type.i32, null)