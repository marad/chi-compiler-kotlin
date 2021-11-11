package gh.marad.chi.core.types

import gh.marad.chi.ast
import gh.marad.chi.core.*
import gh.marad.chi.core.TokenType.KEYWORD
import gh.marad.chi.interpreter.Interpreter
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe

class BoolTypeSpec : FreeSpec({
    "tokenizer" - {
        "should read keywords" {
            tokenize("bool") shouldHaveSingleElement Token(KEYWORD, "bool", Location(0, 0))
            tokenize("true") shouldHaveSingleElement Token(KEYWORD, "true", Location(0, 0))
            tokenize("false") shouldHaveSingleElement Token(KEYWORD, "false", Location(0, 0))
        }
    }

    "parser" - {
        "should read 'true' as a bool value" {
            ast("true") shouldBe Atom.t(Location(1,0))
        }

        "should read 'false' as a bool value" {
            ast("false") shouldBe  Atom.f(Location(1,0))
        }

        "should read bool as type" {
            ast("val x: bool = true")
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        expectedType = Type.bool,
                        immutable = true,
                        location = Location(1, 0),
                        value = Atom.t(Location(1, 14))
                    )
                )
        }
    }

    "interpreter" - {
        "should work with boolean type" {
            val interpreter = Interpreter()
            val result = interpreter.eval("""
                val x = true
                x
            """.trimIndent())!!

            result shouldBe gh.marad.chi.actionast.Atom.t
        }
    }
})