package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.unit
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class ParserSpec : FunSpec({
    test("should read simple name declaration expression") {
        ast("val x = 5")
            .shouldBeTypeOf<NameDeclaration>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeAtom("5", intType)
                it.mutable shouldBe false
                it.expectedType shouldBe null
            }
    }

    test("should read name declaration with expected type definition") {
        ast("val x: int = 5")
            .shouldBeTypeOf<NameDeclaration>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeAtom("5", intType)
                it.mutable shouldBe false
                it.expectedType shouldBe intType
            }
    }

    test("should read function type definition") {
        val scope = CompilationScope(ScopeType.Function, CompilationScope(ScopeType.Package))
        scope.addSymbol("x", fn(unit, intType, intType), SymbolType.Local)
        ast("val foo: (int, int) -> unit = x", scope)
            .shouldBeTypeOf<NameDeclaration>()
            .should {
                it.name shouldBe "foo"
                it.value.shouldBeVariableAccess("x")
                it.mutable shouldBe false
                it.expectedType shouldBe fn(returnType = unit, intType, intType)
            }
    }

    test("should read mutable variable name declaration") {
        ast("var x = 5")
            .shouldBeTypeOf<NameDeclaration>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeAtom("5", intType)
                it.mutable shouldBe true
                it.expectedType shouldBe null
            }
    }

    test("should read basic assignment") {
        val parentScope = CompilationScope(ScopeType.Package)
        ast("x = 5", parentScope)
            .shouldBeTypeOf<Assignment>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeAtom("5", intType)
            }


        ast("x = {}", parentScope)
            .shouldBeTypeOf<Assignment>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeFn { fn ->
                    fn.parameters shouldBe emptyList()
                    fn.returnType shouldBe unit
                    fn.body.shouldBeEmptyBlock()
                }
            }
    }

    test("should read anonymous function expression") {
        ast("{ a: int, b: int -> 0 }", CompilationScope(ScopeType.Package))
            .shouldBeFn {
                it.parameters.should { paramList ->
                    paramList[0].shouldBeFnParam("a", intType)
                    paramList[1].shouldBeFnParam("b", intType)
                }
                it.returnType shouldBe intType
                it.body.body[0].shouldBeAtom("0", intType)
            }
    }

    test("should read variable access through name") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("foo", intType, SymbolType.Local)
        ast("foo", scope)
            .shouldBeVariableAccess("foo")
    }

    test("should read function invocation expression") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("add", fn(intType, intType, intType), SymbolType.Local)
        ast("add(5, 1)", scope)
            .shouldBeTypeOf<FnCall>()
            .should {
                it.function.shouldBeVariableAccess("add")
                it.parameters.should { paramList ->
                    paramList[0].shouldBeAtom("5", intType)
                    paramList[1].shouldBeAtom("1", intType)
                }
            }
    }

    test("should read lambda function invocation expression") {
        val scope = CompilationScope(ScopeType.Package)
        ast("({ 1 })()", scope)
            .shouldBeTypeOf<FnCall>()
            .should {
                it.function.shouldBeTypeOf<Group>().should { group ->
                    group.value.shouldBeFn { fn ->
                        fn.parameters shouldBe emptyList()
                        fn.returnType shouldBe intType
                        fn.body.shouldBeBlock { block ->
                            block.body[0].shouldBeAtom("1", intType)
                        }
                    }
                }
                it.parameters shouldBe emptyList()
            }
    }

    test("should read nested function invocations") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("a", fn(intType, intType), SymbolType.Local)
        scope.addSymbol("b", fn(intType, intType), SymbolType.Local)
        scope.addSymbol("x", intType, SymbolType.Local)
        ast("a(b(x))", scope)
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

    test("should read anonymous function without return type") {
        val scope = CompilationScope(ScopeType.Package)
        ast("{}", scope)
            .shouldBeFn {
                it.returnType shouldBe unit
            }
    }

    test("should skip single line comments") {
        ast(
            """
                // this is a comment
                5
            """.trimIndent()
        ).shouldBeAtom("5", intType)
    }

    test("should skip multiline comments") {
        ast(
            """
                /* this is
                   a multiline comment */
                5   
            """.trimIndent()
        ).shouldBeAtom("5", intType)
    }

    test("should read complex type definition") {
        ast(
            """
                data Foo = Bar(i: int) | Baz
            """.trimIndent()
        ).shouldBeTypeOf<DefineVariantType>().should {
            it.name shouldBe "Foo"
            it.constructors shouldHaveSize 2
            it.constructors[0] should { variant ->
                variant.name shouldBe "Bar"
                variant.fields shouldHaveSize 1
                variant.fields[0].name shouldBe "i"
                variant.fields[0].type shouldBe intType
            }
            it.constructors[1] should { variant ->
                variant.name shouldBe "Baz"
                variant.fields.shouldBeEmpty()
            }
        }
    }

    test("should properly determine value type") {
        ast(
            """
                data Maybe[T] = Just(t: T) | Nothing
                val x = Just(5)
                x
            """.trimIndent()
        ).shouldBeTypeOf<VariableAccess>() should {
            it.type.shouldBeTypeOf<VariantType>() should { variantType ->
                variantType.variant.shouldNotBeNull()
                    .fields[0].type shouldBe intType
            }
        }
    }
})