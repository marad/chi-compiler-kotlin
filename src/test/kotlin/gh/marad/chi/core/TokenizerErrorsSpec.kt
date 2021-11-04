package gh.marad.chi.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class TokenizerErrorsSpec : FunSpec() {
    init {
        test("empty source should result in empty expression list") {
            tokenize("").shouldBeEmpty()
        }

        test("should throw exception on unexpected character") {
            val ex = shouldThrow<UnexpectedCharacter> {
                tokenize("val x = $")
            }
            ex.char.shouldBe('$')
            ex.location.shouldBe(Location(0, 8))
        }
    }
}