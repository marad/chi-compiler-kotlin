package gh.marad.chi.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ParserErrorsSpec : FunSpec() {
    init {
        test("should throw exception on unexpected token on beginning of expression") {
            val ex = shouldThrow<UnexpectedToken> { parse(tokenize("=")) }
            ex.token.value.shouldBe("=")
        }

        test("should throw exception on invalid assignment form") {
            val ex = shouldThrow<UnexpectedToken> { parse(tokenize("val = 5")) }
            ex.token.value.shouldBe("=")
        }

        test("error when parameter definition is missing type") {
            val ex = shouldThrow<UnexpectedToken> { parse(tokenize("fn(a, b:i32) {}")) }
            ex.expected.shouldBe(":")
            ex.token.value.shouldBe(",")
        }

        test("error when type is invalid") {
            val ex = shouldThrow<UnexpectedToken> { parse(tokenize("fn(a: 23){}"))}
            ex.expected.shouldBeNull()
            ex.token.value.shouldBe("23")

            val ex2 = shouldThrow<UnexpectedToken> { parse(tokenize("fn(a: ){}")) }
            ex2.expected.shouldBeNull()
            ex2.token.value.shouldBe(")")
        }

        test("error when missing closing ')' on function definition") {
            val ex = shouldThrow<OneOfTokensExpected> { parse(tokenize("fn(a: i32{}")) }
            ex.expected.shouldContainAll(",", ")")
            ex.actual.value.shouldBe("{")
        }

        test("error on incomplete return type specification") {
            val ex = shouldThrow<UnexpectedToken> { parse(tokenize("fn(): {}")) }
            ex.expected.shouldBeNull()
            ex.token.value.shouldBe("{")
        }

        test("should throw unexpected end of file when trying to read past the last token") {
            val ex = shouldThrow<UnexpectedEndOfFile> { parse(tokenize("foo(4")) }
            ex.location.shouldBe(Location(0, 5))
        }
    }
}