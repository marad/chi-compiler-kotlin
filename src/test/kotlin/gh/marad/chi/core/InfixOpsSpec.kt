package gh.marad.chi.core

import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSingleElement

class InfixOpsSpec : FreeSpec({
    "tokenizer" - {
        "should read infix operators" {
            Tokenizer.infixOperators.forAll {
                tokenize("1 $it 2") shouldContainInOrder listOf(
                    Token(TokenType.INTEGER, "1", Location(0, 0)),
                    Token(TokenType.OPERATOR, it, Location(0, 2)),
                    Token(TokenType.INTEGER, "2", Location(0, it.length + 3)),
                )
            }
        }
    }

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

})