package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.core.analyzer.checkTypes
import gh.marad.chi.interpreter.Interpreter
import gh.marad.chi.interpreter.Value
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class InfixOpsSpec : FreeSpec({
    "parser" - {
        "should read infix operations" {
            parseProgram("1 + 2").expressions shouldHaveSingleElement
                    InfixOp(
                        op = "+",
                        left = Atom.i32(1, Location(1, 0)),
                        right = Atom.i32(2, Location(1, 4)),
                        location = Location(1, 2)
                    )
        }

        "should respect operator precedence" {
            parseProgram("1 + 2 * 3").expressions shouldHaveSingleElement
                    InfixOp(
                        op = "+",
                        left = Atom.i32(1, Location(1, 0)),
                        right = InfixOp(
                            "*",
                            Atom.i32(2, Location(1, 4)),
                            Atom.i32(3, Location(1, 8)),
                            Location(1, 6)
                        ),
                        Location(1, 2)
                    )
        }
    }

    "type checker" - {
        "should check that operation types match" {
            checkTypes(ast("2 + true")) shouldHaveSingleElement
                    TypeMismatch(Type.i32, Type.bool, Location(1, 4))

        }
    }

    "interpreter" - {
        "should calculate arithmetic operations" {
            val interpreter = Interpreter()
            interpreter.eval("2 + 2 * 2")
                .shouldNotBeNull()
                .shouldBe(
                    Value.i32(6)
                )
        }
    }

})