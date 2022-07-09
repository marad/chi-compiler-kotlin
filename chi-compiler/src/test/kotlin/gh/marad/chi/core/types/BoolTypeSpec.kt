package gh.marad.chi.core.types

import gh.marad.chi.ast
import gh.marad.chi.core.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

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
                .shouldBeTypeOf<NameDeclaration>().should {
                    it.name shouldBe "x"
                    it.expectedType shouldBe Type.bool
                    it.immutable shouldBe true
                    it.location shouldBe Location(1, 0)
                    it.value shouldBe Atom.t(Location(1, 14))
                }
        }
    }
})