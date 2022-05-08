package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.asts
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ParserSpec : FunSpec() {
    init {
        test("should read simple name declaration expression") {
            ast("val x = 5")
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", intType, Location(1, 8)),
                        immutable = true,
                        expectedType = null,
                        location = Location(1, 0)
                    )
                )
        }

        test("should read name declaration with expected type definition") {
            ast("val x: int = 5")
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", intType, Location(1, 13)),
                        immutable = true,
                        expectedType = intType,
                        location = Location(1, 0)
                    )
                )
        }

        test("should read function type definition") {
            val scope = CompilationScope()
            scope.addSymbol("x", intType)
            ast("val foo: (int, int) -> unit = x", scope)
                .shouldBe(
                    NameDeclaration(
                        name = "foo",
                        value = VariableAccess(scope, "x", Location(1, 30)),
                        immutable = true,
                        expectedType = Type.fn(returnType = unit, intType, intType),
                        location = Location(1, 0)
                    )
                )
        }

        test("should read mutable variable name declaration") {
            ast("var x = 5")
                .shouldBe(
                    NameDeclaration(
                        name = "x",
                        value = Atom("5", intType, Location(1, 8)),
                        immutable = false,
                        expectedType = null,
                        location = Location(1, 0)
                    )
                )
        }

        test("should read basic assignment") {
            val parentScope = CompilationScope()
            ast("x = 5", parentScope)
                .shouldBe(Assignment(parentScope, "x", Atom("5", intType, Location(1, 4)), Location(1, 2)))

            ast("x = fn() {}", parentScope)
                .shouldBe(Assignment(parentScope,
                    "x",
                    Fn(
                        fnScope = CompilationScope(parent = parentScope),
                        parameters = emptyList(),
                        returnType = unit,
                        block = Block(
                            emptyList(),
                            Location(1, 9)
                        ),
                        location = Location(1, 4)
                    ),
                    Location(1, 2)
                ))
        }

        test("should read anonymous function expression") {
            val scope = CompilationScope()
            ast("fn(a: int, b: int): int {}", scope)
                .shouldBe(
                    Fn(
                        parameters = listOf(FnParam("a", intType, Location(1, 3)), FnParam("b", intType, Location(1, 11))),
                        returnType = intType,
                        block = Block(emptyList(), Location(1, 24)),
                        location = Location(1, 0),
                        fnScope = CompilationScope(parent = scope, symbols = mutableMapOf("a" to intType, "b" to intType))
                    )
                )
        }

        test("should read variable access through name") {
            val scope = CompilationScope()
            ast("foo", scope)
                .shouldBe(VariableAccess(scope, "foo", Location(1, 0)))
        }

        test("should read function invocation expression") {
            val scope = CompilationScope()
            ast("add(5, 1)", scope)
                .shouldBe(
                    FnCall(
                        enclosingScope = scope,
                        name = "add",
                        parameters = listOf(
                            Atom("5", intType, Location(1, 4)),
                            Atom("1", intType, Location(1, 7))
                        ),
                        location = Location(1, 0)
                    )
                )
        }

        test("should read nested function invocations") {
            ast("a(b(x))")
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
            val scope = CompilationScope()
            ast("fn(): int {}", scope)
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = intType,
                        block = Block(emptyList(), Location(1, 10)),
                        location = Location(1, 0),
                        fnScope = CompilationScope(parent = scope),
                    )
                )
        }

        test("should read anonymous function without return type") {
            val scope = CompilationScope()
            ast("fn() {}", scope)
                .shouldBe(
                    Fn(
                        parameters = emptyList(),
                        returnType = unit,
                        block = Block(emptyList(), Location(1, 5)),
                        location = Location(1, 0),
                        fnScope = CompilationScope(parent = scope),
                    )
                )
        }

        test("should read if expression") {
            ast("if(1) { 2 } else { 3 }")
                .shouldBe(
                    IfElse(
                        location = Location(1, 0),
                        condition = Atom("1", intType, Location(1, 3)),
                        thenBranch = Block(
                            listOf(Atom("2", intType, Location(1, 8))),
                            Location(1, 6)
                            ),
                        elseBranch = Block(
                            listOf(Atom("3", intType, Location(1, 19))),
                            Location(1, 17)
                        )
                    )
                )
        }

        test("else branch should be optional") {
            ast("if(1) { 2 }")
                .shouldBe(
                    IfElse(
                        location = Location(1, 0),
                        condition = Atom("1", intType, Location(1, 3)),
                        thenBranch = Block(
                            listOf(Atom("2", intType, Location(1, 8))),
                            Location(1, 6)
                        ),
                        elseBranch = null
                    )
                )
        }

        test("should skip single line comments") {
            asts("""
                // this is a comment
                5
            """.trimIndent()) shouldHaveSingleElement
                    Atom.int(5, Location(2, 0))
        }

        test("should skip multiline comments") {
            asts("""
                /* this is
                   a multiline comment */
                5   
            """.trimIndent()) shouldHaveSingleElement
                    Atom.int(5, Location(3, 0))

        }
    }
}