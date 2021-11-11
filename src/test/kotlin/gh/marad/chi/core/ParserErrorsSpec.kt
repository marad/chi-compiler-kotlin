package gh.marad.chi.core

import gh.marad.chi.ast
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ParserErrorsSpec : FunSpec() {
    init {
        // TODO: fix error exceptions
//        test("should throw exception on unexpected token on beginning of expression") {
//            val ex = shouldThrow<UnexpectedToken> { ast("=") }
//            ex.token.value.shouldBe("=")
//        }
//
//        test("should throw exception on invalid declaration form") {
//            val ex = shouldThrow<UnexpectedToken> { ast("val = 5") }
//            ex.token.value.shouldBe("=")
//        }
//
//        test("error when parameter definition is missing type") {
//            val ex = shouldThrow<UnexpectedToken> { ast("fn(a, b:i32) {}") }
//            ex.expected.shouldBe(":")
//            ex.token.value.shouldBe(",")
//        }
//
//        test("error when type is invalid") {
//            val ex = shouldThrow<UnexpectedToken> { ast("fn(a: 23){}")}
//            ex.expected.shouldBeNull()
//            ex.token.value.shouldBe("23")
//
//            val ex2 = shouldThrow<UnexpectedToken> { ast("fn(a: ){}") }
//            ex2.expected.shouldBeNull()
//            ex2.token.value.shouldBe(")")
//        }
//
//        test("error when missing closing ')' on function definition") {
//            val ex = shouldThrow<OneOfTokensExpected> { ast("fn(a: i32{}") }
//            ex.expected.shouldContainAll(",", ")")
//            ex.actual.value.shouldBe("{")
//        }
//
//        test("error on incomplete return type specification") {
//            val ex = shouldThrow<UnexpectedToken> { ast("fn(): {}") }
//            ex.expected.shouldBeNull()
//            ex.token.value.shouldBe("{")
//        }
//
//        test("should throw unexpected end of file when trying to read past the last token") {
//            val ex = shouldThrow<UnexpectedEndOfFile> { ast("foo(4") }
//            ex.location.shouldBe(Location(0, 5))
//        }
    }
}