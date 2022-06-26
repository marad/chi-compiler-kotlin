package gh.marad.chi.core

import gh.marad.chi.ast
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSingleElement

class InfixOpsSpec : FreeSpec({
    "parser" - {
        "should read infix operations" {
            parseProgram("1 + 2").first.expressions shouldHaveSingleElement
                    InfixOp(
                        op = "+",
                        left = Atom.int(1, Location(1, 0)),
                        right = Atom.int(2, Location(1, 4)),
                        location = Location(1, 2)
                    )
        }

        "should respect operator precedence" {
            parseProgram("1 + 2 * 3").first.expressions shouldHaveSingleElement
                    InfixOp(
                        op = "+",
                        left = Atom.int(1, Location(1, 0)),
                        right = InfixOp(
                            "*",
                            Atom.int(2, Location(1, 4)),
                            Atom.int(3, Location(1, 8)),
                            Location(1, 6)
                        ),
                        Location(1, 2)
                    )
        }
    }

    "type checker" - {
        "should check that operation types match" {
            analyze(ast("2 + true")) shouldHaveSingleElement
                    TypeMismatch(Type.intType, Type.bool, Location(1, 4))

        }
    }

})