package gh.marad.chi.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParserSpec : FunSpec() {
    init {
        test("should read simple assignment expression") {
            parse(tokenize("val x = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32, Location(0, 8)),
                        immutable = true,
                        expectedType = null,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read assignment with expected type definition") {
            parse(tokenize("val x: i32 = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32, Location(0, 13)),
                        immutable = true,
                        expectedType = Type.i32,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read mutable variable assignment") {
            parse(tokenize("var x = 5"))
                .first()
                .shouldBe(
                    Assignment(
                        name = "x",
                        value = Atom("5", Type.i32, Location(0, 8)),
                        immutable = false,
                        expectedType = null,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read anonymous function expression") {
            parse(tokenize("fn(a: i32, b: i32): i32 {}"))
                .first()
                .shouldBe(
                    Fn(
                        parameters = listOf(FnParam("a", Type.i32), FnParam("b", Type.i32)),
                        returnType = Type.i32,
                        body = BlockExpression(emptyList(), Location(0, 24)),
                        location = Location(0, 0)
                    )
                )
        }

        test("should read variable access through name") {
            parse(tokenize("foo"))
                .first()
                .shouldBe(VariableAccess("foo", Location(0, 0)))
        }

        test("should read function invocation expression") {
            parse(tokenize("add(5, 1)"))
                .first()
                .shouldBe(
                    FnCall(
                        name = "add",
                        parameters = listOf(
                            Atom("5", Type.i32, Location(0, 4)),
                            Atom("1", Type.i32, Location(0, 7))
                        ),
                        location = Location(0, 0)
                    )
                )
        }

        test("should read nested function invocations") {
            parse(tokenize("a(b(c(d(x))))"))
                .first()
                .shouldBe(
                    FnCall(
                        name = "a",
                        location = Location(0, 0),
                        parameters = listOf(
                            FnCall(
                                name = "b",
                                location = Location(0, 2),
                                parameters = listOf(
                                    FnCall(
                                        name = "c",
                                        location = Location(0, 4),
                                        parameters = listOf(
                                            FnCall(
                                                name = "d",
                                                location = Location(0, 6),
                                                parameters = listOf(VariableAccess("x", Location(0, 8))),
                                            )
                                        ),
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
                        body = BlockExpression(emptyList(), Location(0, 10)),
                        location = Location(0, 0)
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
                        body = BlockExpression(emptyList(), Location(0, 5)),
                        location = Location(0, 0)
                    )
                )
        }
    }
}