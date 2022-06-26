package gh.marad.chi.core.types

import gh.marad.chi.ast
import gh.marad.chi.core.*
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
                        value = Atom.t(Location(1, 14)),
                        enclosingScope = CompilationScope().apply {
                            addSymbol("x", Type.bool, SymbolScope.Local)
                        }
                    )
                )
        }
    }
})