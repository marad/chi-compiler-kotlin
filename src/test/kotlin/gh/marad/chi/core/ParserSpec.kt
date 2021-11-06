package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParserSpec : FunSpec() {
    init {
        test("should read simple name declaration expression") {
            parse(tokenize("val x = 5"))
                .first()
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", i32, Location(0, 8)),
                        immutable = true,
                        expectedType = null,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read name declaration with expected type definition") {
            parse(tokenize("val x: i32 = 5"))
                .first()
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", i32, Location(0, 13)),
                        immutable = true,
                        expectedType = i32,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read function type definition") {
            parse(tokenize("val foo: (i32, i32) -> unit = fn(a: i32, b: i32) {}"))
                .first()
                .shouldBe(
                    NameDeclaration(
                        name = "foo",
                        value = Fn(
                            parameters = listOf(
                                FnParam("a", i32, Location(0, 33)),
                                FnParam("b", i32, Location(0, 41)),
                            ),
                            returnType = unit,
                            block = Block(emptyList(), Location(0, 49)),
                            location = Location(0, 30)
                        ),
                        immutable = true,
                        expectedType = Type.fn(returnType = unit, i32, i32),
                        location = Location(0, 0)
                    )
                )
        }

        test("should read mutable variable name declaration") {
            parse(tokenize("var x = 5"))
                .first()
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", i32, Location(0, 8)),
                        immutable = false,
                        expectedType = null,
                        location = Location(0, 0)
                    )
                )
        }

        test("should read basic assignment") {
            parse(tokenize("x = 5"))
                .first()
                .shouldBe(Assignment("x", Atom("5", i32, Location(0, 4)), Location(0, 2)))

            parse(tokenize("x = fn() {}"))
                .first()
                .shouldBe(Assignment("x",
                    Fn(emptyList(),
                        unit,
                        Block(
                            emptyList(),
                            Location(0, 9)
                        ),
                        Location(0, 4)
                    ),
                    Location(0, 2)
                ))
        }

        test("should read anonymous function expression") {
            parse(tokenize("fn(a: i32, b: i32): i32 {}"))
                .first()
                .shouldBe(
                    Fn(
                        parameters = listOf(FnParam("a", i32, Location(0, 3)), FnParam("b", i32, Location(0, 11))),
                        returnType = i32,
                        block = Block(emptyList(), Location(0, 24)),
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
                            Atom("5", i32, Location(0, 4)),
                            Atom("1", i32, Location(0, 7))
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
                        returnType = i32,
                        block = Block(emptyList(), Location(0, 10)),
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
                        returnType = unit,
                        block = Block(emptyList(), Location(0, 5)),
                        location = Location(0, 0)
                    )
                )
        }

        test("should read if expression") {
            parse(tokenize("if(1) { 2 } else { 3 }"))
                .first()
                .shouldBe(
                    IfElse(
                        location = Location(0, 0),
                        condition = Atom("1", i32, Location(0, 3)),
                        thenBranch = Block(
                            listOf(Atom("2", i32, Location(0, 8))),
                            Location(0, 6)
                            ),
                        elseBranch = Block(
                            listOf(Atom("3", i32, Location(0, 19))),
                            Location(0, 17)
                        )
                    )
                )
        }

        test("else branch should be optional") {
            parse(tokenize("if(1) { 2 }"))
                .first()
                .shouldBe(
                    IfElse(
                        location = Location(0, 0),
                        condition = Atom("1", i32, Location(0, 3)),
                        thenBranch = Block(
                            listOf(Atom("2", i32, Location(0, 8))),
                            Location(0, 6)
                        ),
                        elseBranch = null
                    )
                )
        }
    }
}