package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

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
            val scope = NewScope()
            parse(tokenize("val foo: (i32, i32) -> unit = x"), scope)
                .first()
                .shouldBe(
                    NameDeclaration(
                        name = "foo",
                        value = VariableAccess(scope, "x", Location(0, 30)),
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
            val parentScope = NewScope()
            parse(tokenize("x = 5"), parentScope)
                .first()
                .shouldBe(Assignment(parentScope, "x", Atom("5", i32, Location(0, 4)), Location(0, 2)))

            parse(tokenize("x = fn() {}"), parentScope)
                .first()
                .shouldBe(Assignment(parentScope,
                    "x",
                    Fn(
                        fnScope = NewScope(parent = parentScope),
                        parameters = emptyList(),
                        returnType = unit,
                        block = Block(
                            emptyList(),
                            Location(0, 9)
                        ),
                        location = Location(0, 4)
                    ),
                    Location(0, 2)
                ))
        }

        test("should read anonymous function expression") {
            val scope = NewScope()
            parse(tokenize("fn(a: i32, b: i32): i32 {}"), scope)
                .first()
                .shouldBe(
                    Fn(
                        parameters = listOf(FnParam("a", i32, Location(0, 3)), FnParam("b", i32, Location(0, 11))),
                        returnType = i32,
                        block = Block(emptyList(), Location(0, 24)),
                        location = Location(0, 0),
                        fnScope = NewScope(parent=scope)
                    )
                )
        }

        test("should read variable access through name") {
            val scope = NewScope()
            parse(tokenize("foo"), scope)
                .first()
                .shouldBe(VariableAccess(scope, "foo", Location(0, 0)))
        }

        test("should read function invocation expression") {
            val scope = NewScope()
            parse(tokenize("add(5, 1)"), scope)
                .first()
                .shouldBe(
                    FnCall(
                        enclosingScope = scope,
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
            parse(tokenize("a(b(x))")).first()
                .shouldBeTypeOf<FnCall>()
                .should { aFnCall ->
                    aFnCall.parameters
                        .shouldHaveSize(1)
                        .first()
                        .shouldBeTypeOf<FnCall>().should { bFnCall ->
                            bFnCall.parameters
                                .shouldHaveSize(1)
                                .first()
                                .shouldBeTypeOf<VariableAccess>()
                                .should {
                                    it.name.shouldBe("x")
                                }
                        }
                }
        }

        test("should read anonymous function without parameters") {
            val scope = NewScope()
            parse(tokenize("fn(): i32 {}"), scope)
                .first()
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = i32,
                        block = Block(emptyList(), Location(0, 10)),
                        location = Location(0, 0),
                        fnScope = NewScope(parent=scope),
                    )
                )
        }

        test("should read anonymous function without return type") {
            val scope = NewScope()
            parse(tokenize("fn() {}"), scope)
                .first()
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = unit,
                        block = Block(emptyList(), Location(0, 5)),
                        location = Location(0, 0),
                        fnScope = NewScope(parent=scope),
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