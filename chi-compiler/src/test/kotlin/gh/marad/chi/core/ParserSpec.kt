package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.asts
import gh.marad.chi.core.Type.Companion.fn
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
                .shouldBeTypeOf<NameDeclaration>()
                .should {
                    it.name shouldBe "x"
                    it.value.shouldBeAtom("5", intType, Location(1, 8))
                    it.immutable shouldBe true
                    it.expectedType shouldBe null
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read name declaration with expected type definition") {
            ast("val x: int = 5")
                .shouldBeTypeOf<NameDeclaration>()
                .should {
                    it.name shouldBe "x"
                    it.value.shouldBeAtom("5", intType, Location(1, 13))
                    it.immutable shouldBe true
                    it.expectedType shouldBe intType
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read function type definition") {
            val scope = CompilationScope(CompilationScope())
            scope.addSymbol("x", intType, SymbolScope.Local)
            ast("val foo: (int, int) -> unit = x", scope)
                .shouldBeTypeOf<NameDeclaration>()
                .should {
                    it.name shouldBe "foo"
                    it.value.shouldBeVariableAccess("x", Location(1, 30))
                    it.immutable shouldBe true
                    it.expectedType shouldBe fn(returnType = unit, intType, intType)
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read mutable variable name declaration") {
            ast("var x = 5")
                .shouldBeTypeOf<NameDeclaration>()
                .should {
                    it.name shouldBe "x"
                    it.value.shouldBeAtom("5", intType, Location(1, 8))
                    it.immutable shouldBe false
                    it.expectedType shouldBe null
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read basic assignment") {
            val parentScope = CompilationScope()
            ast("x = 5", parentScope)
                .shouldBeTypeOf<Assignment>()
                .should {
                    it.name shouldBe "x"
                    it.value.shouldBeAtom("5", intType, Location(1, 4))
                    it.location shouldBe Location(1, 2)
                }


            ast("x = fn() {}", parentScope)
                .shouldBeTypeOf<Assignment>()
                .should {
                    it.name shouldBe "x"
                    it.value.shouldBeFn { fn ->
                        fn.parameters shouldBe emptyList()
                        fn.returnType shouldBe unit
                        fn.body shouldBe Block(emptyList(), Location(1, 9))
                        fn.location shouldBe Location(1, 4)
                    }
                    it.location shouldBe Location(1, 2)
                }
        }

        test("should read group expression") {
            ast("(1 + 2)")
                .shouldBe(
                    Group(
                        InfixOp("+", Atom.int(1, Location(1, 1)), Atom.int(2, Location(1, 5),), Location(1, 3)),
                        Location(1, 0)
                    )
                )
        }

        test("should read anonymous function expression") {
            ast("fn(a: int, b: int): int {}", CompilationScope())
                .shouldBeFn {
                    it.parameters shouldBe listOf(
                        FnParam("a", intType, Location(1, 3)),
                        FnParam("b", intType, Location(1, 11)))
                    it.returnType shouldBe intType
                    it.body shouldBe Block(emptyList(), Location(1, 24))
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read variable access through name") {
            val scope = CompilationScope()
            ast("foo", scope)
                .shouldBeVariableAccess("foo", Location(1, 0))
        }

        test("should read function invocation expression") {
            val scope = CompilationScope()
            ast("add(5, 1)", scope)
                .shouldBeTypeOf<FnCall>()
                .should {
                    it.name shouldBe "add"
                    it.function.shouldBeVariableAccess("add", Location(1, 0))
                    it.parameters shouldBe listOf(
                        Atom("5", intType, Location(1, 4)),
                        Atom("1", intType, Location(1, 7))
                    )
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read lambda function invocation expression") {
            val scope = CompilationScope()
            ast("(fn() { 1 })()", scope)
                .shouldBeTypeOf<FnCall>()
                .should {
                    it.name shouldBe "(fn(){1})"
                    it.function.shouldBeTypeOf<Group>().should { group ->
                        group.value.shouldBeFn { fn ->
                            fn.parameters shouldBe emptyList()
                            fn.returnType shouldBe unit
                            fn.body shouldBe Block(listOf(Atom.int(1, Location(1, 8))), Location(1, 6))
                            fn.location shouldBe Location(1, 1)
                        }
                        group.location shouldBe Location(1, 0)
                    }
                    it.parameters shouldBe emptyList()
                    it.location shouldBe Location(1, 0)
                }
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
                .shouldBeFn {
                    it.parameters shouldBe emptyList()
                    it.returnType shouldBe intType
                    it.body shouldBe Block(emptyList(), Location(1, 10))
                    it.location shouldBe Location(1, 0)
                }
        }

        test("should read anonymous function without return type") {
            val scope = CompilationScope()
            ast("fn() {}", scope)
                .shouldBeFn {
                    it.returnType shouldBe unit
                }
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