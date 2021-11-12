package gh.marad.chi.core.types

import gh.marad.chi.ast
import gh.marad.chi.core.Atom
import gh.marad.chi.core.Location
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type
import gh.marad.chi.interpreter.Interpreter
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BoolTypeSpec : FreeSpec({
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