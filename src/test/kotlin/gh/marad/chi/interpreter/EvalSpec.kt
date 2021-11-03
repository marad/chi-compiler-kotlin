package gh.marad.chi.interpreter

import gh.marad.chi.core.*
import gh.marad.chi.interpreter.Scope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldNotContainAnyKeysOf
import io.kotest.matchers.shouldBe

class EvalSpec : FunSpec() {

    init {
        test("should eval simple atom to itself") {
            // given
            val scope = Scope()

            // expect
            scope.eval(intAtom)
                .shouldBe(intAtom)
        }

        test("evaling variable read should return value from scope") {
            // given
            val scope = Scope(mapOf("foo" to intAtom))

            // expect
            scope.eval(VariableAccess("foo"))
                .shouldBe(intAtom)
        }

        test("evaluating assignment should update scope") {
            // given
            val scope = Scope()

            // when
            val result = scope.eval(Assignment("x", intAtom, immutable = true, expectedType = Type.i32))

            // then
            result.shouldBe(intAtom)
            scope.names.shouldContain("x", intAtom)
        }

        test("function definition should evaluate to itself") {
            // given
            val scope = Scope()
            val emptyFn = Fn(parameters = emptyList(), returnType = Type.i32, BlockExpression(emptyList()))

            // expect
            scope.eval(emptyFn)
                .shouldBe(emptyFn)
        }

        test("block expression should return last expression value") {
            // given
            val scope = Scope()
            val lastAtom = Atom("10", Type.i32)

            // when
            val result = scope.eval(BlockExpression(listOf(
                Assignment("x", intAtom, immutable = true, expectedType = Type.i32),
                Assignment("y", lastAtom, immutable = false, expectedType = Type.i32)
            )))

            // then
            result.shouldBe(lastAtom)
        }


        test("block expression should evaluate all expressions") {
            // given
            val scope = Scope()
            val lastAtom = Atom("10", Type.i32)

            // when
            scope.eval(BlockExpression(listOf(
                Assignment("x", intAtom, immutable = true, expectedType = Type.i32),
                Assignment("y", lastAtom, immutable = false, expectedType = Type.i32)
            )))

            // then
            scope.names.shouldContainAll(mapOf(
                "x" to intAtom,
                "y" to lastAtom
            ))
        }

        test("empty block expression returns unit") {
            // given
            val scope = Scope()

            // when
            val result = scope.eval(BlockExpression(emptyList()))

            // then
            result.shouldBe(Atom("()", Type.unit))
        }

        test("calling function should evaluate its body") {
            // given
            val lastAtom = Atom("10", Type.i32)
            val body = BlockExpression(listOf(
                Assignment("x", intAtom, immutable = true, expectedType = Type.i32),
                Assignment("y", lastAtom, immutable = false, expectedType = Type.i32)
            ))
            val fn = Fn(
                parameters = listOf(
                    FnParam("a", Type.i32)
                ),
                returnType = Type.i32,
                body = body
            )
            val scope = Scope(mapOf(
                "foo" to fn
            ))

            // when
            val result = scope.eval(
                FnCall("foo", listOf(Atom("1", Type.i32)))
            )

            // then result is the value of last expression
            result.shouldBe(lastAtom)

            // and parent scope should not change
            scope.names.shouldNotContainAnyKeysOf("x", "y")
        }
    }
}

private val intAtom = Atom("5", Type.i32)