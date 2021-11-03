package gh.marad.chi.core

import gh.marad.chi.core.TokenType.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder

class TokenizerSpec : FunSpec() {
    init {
        test("should tokenize simple variable creation") {
            tokenize("val x = 5")
                .shouldContainInOrder(
                    Token(KEYWORD, "val"),
                    Token(SYMBOL, "x"),
                    Token(OPERATOR, "="),
                    Token(INTEGER, "5")
                )

            tokenize("var x = 5")
                .shouldContainInOrder(
                    Token(KEYWORD, "var"),
                    Token(SYMBOL, "x"),
                    Token(OPERATOR, "="),
                    Token(INTEGER, "5")
                )
        }

        test("should tokenize simple anonymous function definition") {
            tokenize("fn(a: i32, b: i32): i32 { }")
                .shouldContainInOrder(
                    Token(KEYWORD, "fn"),
                    Token(OPERATOR, "("),
                    Token(SYMBOL, "a"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ","),
                    Token(SYMBOL, "b"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ")"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, "{"),
                    Token(OPERATOR, "}"),
                )
        }

        test("should tokenize function call") {
            tokenize("foo(5, bar)")
                .shouldContainInOrder(
                    Token(SYMBOL, "foo"),
                    Token(OPERATOR, "("),
                    Token(INTEGER, "5"),
                    Token(OPERATOR, ","),
                    Token(SYMBOL, "bar"),
                    Token(OPERATOR, ")"),
                )
        }

        test("should tokenize complex function definition") {
            tokenize("""
                val add = fn(a: i32, b: i32): i32 {
                    plus(1, 2)
                }
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val"),
                    Token(SYMBOL, "add"),
                    Token(OPERATOR, "="),
                    Token(KEYWORD, "fn"),
                    Token(OPERATOR, "("),
                    Token(SYMBOL, "a"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ","),
                    Token(SYMBOL, "b"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ")"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, "{"),
                    Token(SYMBOL, "plus"),
                    Token(OPERATOR, "("),
                    Token(INTEGER, "1"),
                    Token(OPERATOR, ","),
                    Token(INTEGER, "2"),
                    Token(OPERATOR, ")"),
                    Token(OPERATOR, "}"),
                )
        }

        test("should not require whitespace") {
            tokenize("""
                val add=fn(a:i32,b:i32):i32{plus(1, 2)}
            """.trimIndent())
                .shouldContainInOrder(
                    Token(KEYWORD, "val"),
                    Token(SYMBOL, "add"),
                    Token(OPERATOR, "="),
                    Token(KEYWORD, "fn"),
                    Token(OPERATOR, "("),
                    Token(SYMBOL, "a"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ","),
                    Token(SYMBOL, "b"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, ")"),
                    Token(OPERATOR, ":"),
                    Token(KEYWORD, "i32"),
                    Token(OPERATOR, "{"),
                    Token(SYMBOL, "plus"),
                    Token(OPERATOR, "("),
                    Token(INTEGER, "1"),
                    Token(OPERATOR, ","),
                    Token(INTEGER, "2"),
                    Token(OPERATOR, ")"),
                    Token(OPERATOR, "}"),
                )
        }

    }
}