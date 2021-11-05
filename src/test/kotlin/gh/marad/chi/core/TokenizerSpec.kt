package gh.marad.chi.core

import gh.marad.chi.core.TokenType.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainInOrder

class TokenizerSpec : FunSpec() {
    init {
        test("should tokenize simple variable creation") {
            tokenize("val x = 5")
                .shouldContainInOrder(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "x", Location(0, 4)),
                    Token(OPERATOR, "=", Location(0, 6)),
                    Token(INTEGER, "5", Location(0, 8))
                )

            tokenize("var x = 5")
                .shouldContainInOrder(
                    Token(KEYWORD, "var", Location(0, 0)),
                    Token(SYMBOL, "x", Location(0, 4)),
                    Token(OPERATOR, "=", Location(0, 6)),
                    Token(INTEGER, "5", Location(0, 8))
                )
        }

        test("should tokenize simple anonymous function definition") {
            tokenize("fn(a: i32, b: i32): i32 { }")
                .shouldContainInOrder(
                    Token(KEYWORD, "fn", Location(0, 0)),
                    Token(OPERATOR, "(", Location(0, 2)),
                    Token(SYMBOL, "a", Location(0, 3)),
                    Token(OPERATOR, ":", Location(0, 4)),
                    Token(KEYWORD, "i32", Location(0, 6)),
                    Token(OPERATOR, ",", Location(0, 9)),
                    Token(SYMBOL, "b", Location(0, 11)),
                    Token(OPERATOR, ":", Location(0, 12)),
                    Token(KEYWORD, "i32", Location(0, 14)),
                    Token(OPERATOR, ")", Location(0, 17)),
                    Token(OPERATOR, ":", Location(0, 18)),
                    Token(KEYWORD, "i32", Location(0, 20)),
                    Token(OPERATOR, "{", Location(0, 24)),
                    Token(OPERATOR, "}", Location(0, 26)),
                )
        }

        test("should tokenize function call") {
            tokenize("foo(5, bar)")
                .shouldContainInOrder(
                    Token(SYMBOL, "foo", Location(0, 0)),
                    Token(OPERATOR, "(", Location(0, 3)),
                    Token(INTEGER, "5", Location(0, 4)),
                    Token(OPERATOR, ",", Location(0, 5)),
                    Token(SYMBOL, "bar", Location(0, 7)),
                    Token(OPERATOR, ")", Location(0, 10)),
                )
        }

        test("should tokenize complex function definition") {
            tokenize("""
                val add = fn(a: i32, b: i32): i32 {
                    plus(1, 2)
                }
            """.trimIndent())
                .shouldContainAll(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "add", Location(0, 4)),
                    Token(OPERATOR, "=", Location(0, 8)),
                    Token(KEYWORD, "fn", Location(0, 10)),
                    Token(OPERATOR, "(", Location(0, 12)),
                    Token(SYMBOL, "a", Location(0, 13)),
                    Token(OPERATOR, ":", Location(0, 14)),
                    Token(KEYWORD, "i32", Location(0, 16)),
                    Token(OPERATOR, ",", Location(0, 19)),
                    Token(SYMBOL, "b", Location(0, 21)),
                    Token(OPERATOR, ":", Location(0, 22)),
                    Token(KEYWORD, "i32", Location(0, 24)),
                    Token(OPERATOR, ")", Location(0, 27)),
                    Token(OPERATOR, ":", Location(0, 28)),
                    Token(KEYWORD, "i32", Location(0, 30)),
                    Token(OPERATOR, "{", Location(0, 34)),
                    Token(SYMBOL, "plus", Location(1, 4)),
                    Token(OPERATOR, "(", Location(1, 8)),
                    Token(INTEGER, "1", Location(1, 9)),
                    Token(OPERATOR, ",", Location(1, 10)),
                    Token(INTEGER, "2", Location(1, 12)),
                    Token(OPERATOR, ")", Location(1, 13)),
                    Token(OPERATOR, "}", Location(2, 0)),
                )
        }

        test("should not require whitespace") {
            tokenize("""
                val add=fn(a:i32,b:i32):i32{plus(1, 2)}
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "add", Location(0, 4)),
                    Token(OPERATOR, "=", Location(0, 7)),
                    Token(KEYWORD, "fn", Location(0, 8)),
                    Token(OPERATOR, "(", Location(0, 10)),
                    Token(SYMBOL, "a", Location(0, 11)),
                    Token(OPERATOR, ":", Location(0, 12)),
                    Token(KEYWORD, "i32", Location(0, 13)),
                    Token(OPERATOR, ",", Location(0, 16)),
                    Token(SYMBOL, "b", Location(0, 17)),
                    Token(OPERATOR, ":", Location(0, 18)),
                    Token(KEYWORD, "i32", Location(0, 19)),
                    Token(OPERATOR, ")", Location(0, 22)),
                    Token(OPERATOR, ":", Location(0, 23)),
                    Token(KEYWORD, "i32", Location(0, 24)),
                    Token(OPERATOR, "{", Location(0, 27)),
                    Token(SYMBOL, "plus", Location(0, 28)),
                    Token(OPERATOR, "(", Location(0, 32)),
                    Token(INTEGER, "1", Location(0, 33)),
                    Token(OPERATOR, ",", Location(0, 34)),
                    Token(INTEGER, "2", Location(0, 36)),
                    Token(OPERATOR, ")", Location(0, 37)),
                    Token(OPERATOR, "}", Location(0, 38)),
                )
        }

        test("should read function type") {
            tokenize("""
                val main: () -> unit
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "main", Location(0, 4)),
                    Token(OPERATOR, ":", Location(0, 8)),
                    Token(OPERATOR, "(", Location(0, 10)),
                    Token(OPERATOR, ")", Location(0, 11)),
                    Token(OPERATOR, "->", Location(0, 13)),
                    Token(KEYWORD, "unit", Location(0, 16)),
                )

            tokenize("""
                val main: (i32) -> unit
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "main", Location(0, 4)),
                    Token(OPERATOR, ":", Location(0, 8)),
                    Token(OPERATOR, "(", Location(0, 10)),
                    Token(KEYWORD, "i32", Location(0, 11)),
                    Token(OPERATOR, ")", Location(0, 14)),
                    Token(OPERATOR, "->", Location(0, 16)),
                    Token(KEYWORD, "unit", Location(0, 19)),
                )

            tokenize("""
                val main: (i32, i32) -> unit
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val", Location(0, 0)),
                    Token(SYMBOL, "main", Location(0, 4)),
                    Token(OPERATOR, ":", Location(0, 8)),
                    Token(OPERATOR, "(", Location(0, 10)),
                    Token(KEYWORD, "i32", Location(0, 11)),
                    Token(OPERATOR, ",", Location(0, 14)),
                    Token(KEYWORD, "i32", Location(0, 16)),
                    Token(OPERATOR, ")", Location(0, 19)),
                    Token(OPERATOR, "->", Location(0, 21)),
                    Token(KEYWORD, "unit", Location(0, 24)),
                )
        }
    }
}