package gh.marad.chi.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class ParserSpec : FunSpec() {
    init {
        test("should read simple assignment expression") {
            parse(tokenize("val x = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32),
                        immutable = true,
                        expectedType = null
                    )
                )
        }

        test("should read assignment with expected type definition") {
            parse(tokenize("val x: i32 = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32),
                        immutable = true,
                        expectedType = Type.i32
                    )
                )
        }

        test("should read mutable variable assignment") {
            parse(tokenize("var x = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32),
                        immutable = false,
                        expectedType = null
                    )
                )
        }

        test("should read anonymous function expression") {
            parse(tokenize("fn(a: i32, b: i32): i32 {}"))
                .shouldContain(
                    Fn(
                        parameters = listOf(FnParam("a", Type.i32), FnParam("b", Type.i32)),
                        returnType = Type.i32,
                        body = BlockExpression(emptyList())
                    )
                )
        }

        test("should read variable access through name") {
            parse(tokenize("foo"))
                .shouldContain(VariableAccess("foo"))
        }

        test("should read function invocation expression") {
            parse(tokenize("add(5, 1)"))
                .shouldContain(
                    FnCall(
                        name = "add",
                        parameters = listOf(
                            Atom("5", Type.i32),
                            Atom("1", Type.i32)
                        )
                    )
                )
        }

        test("should read nested function invocations") {
            parse(tokenize("a(b(c(d(x))))"))
                .first()
                .shouldBe(
                    FnCall(
                        name = "a",
                        parameters = listOf(
                            FnCall(
                                name = "b",
                                parameters = listOf(
                                    FnCall(
                                        name = "c",
                                        parameters = listOf(
                                            FnCall(
                                                name = "d",
                                                parameters = listOf(VariableAccess("x"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
        }

        test("should read anonymous function without parameters") {
            parse(tokenize("fn(): i32 {}"))
                .first()
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = Type.i32,
                        body = BlockExpression(emptyList())
                    )
                )
        }

        test("should read anonymous function without return type") {
            parse(tokenize("fn() {}"))
                .first()
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = Type.unit,
                        body = BlockExpression(emptyList())
                    )
                )
        }
    }
}