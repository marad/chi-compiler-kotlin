package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.unit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

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
        val scope = CompilationScope(CompilationScope())
        scope.addSymbol("x", fn(unit, intType, intType), SymbolScope.Local)
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
        val parentScope = CompilationScope()
        ast("x = 5", parentScope)
            .shouldBeTypeOf<Assignment>()
            .should {
                it.name shouldBe "x"
                it.value.shouldBeAtom("5", intType)
            }


        ast("x = fn() {}", parentScope)
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

    test("should read group expression") {
        ast("(1 + 2)")
            .shouldBeTypeOf<Group>().should { group ->
                group.value.shouldBeTypeOf<InfixOp>().should { op ->
                    op.op shouldBe "+"
                    op.left.shouldBeAtom("1", intType)
                    op.right.shouldBeAtom("2", intType)
                }
            }
    }

    test("should read anonymous function expression") {
        ast("fn(a: int, b: int): int { 0 }", CompilationScope())
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
        val scope = CompilationScope()
        scope.addSymbol("foo", intType, SymbolScope.Local)
        ast("foo", scope)
            .shouldBeVariableAccess("foo")
    }

    test("should read function invocation expression") {
        val scope = CompilationScope()
        scope.addSymbol("add", fn(intType, intType, intType), SymbolScope.Local)
        ast("add(5, 1)", scope)
            .shouldBeTypeOf<FnCall>()
            .should {
                it.name shouldBe "add"
                it.function.shouldBeVariableAccess("add")
                it.parameters.should { paramList ->
                    paramList[0].shouldBeAtom("5", intType)
                    paramList[1].shouldBeAtom("1", intType)
                }
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
                        fn.body.shouldBeBlock { block ->
                            block.body[0].shouldBeAtom("1", intType)
                        }
                    }
                }
                it.parameters shouldBe emptyList()
            }
    }

    test("should read nested function invocations") {
        val scope = CompilationScope()
        scope.addSymbol("a", fn(intType, intType), SymbolScope.Local)
        scope.addSymbol("b", fn(intType, intType), SymbolScope.Local)
        scope.addSymbol("x", intType, SymbolScope.Local)
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

    test("should read anonymous function without parameters") {
        val scope = CompilationScope()
        ast("fn(): int { 0 }", scope)
            .shouldBeFn {
                it.parameters shouldBe emptyList()
                it.returnType shouldBe intType
                it.body.body[0].shouldBeAtom("0", intType)
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
        ast("if(true) { 2 } else { 3 }")
            .shouldBeTypeOf<IfElse>().should {
                it.condition.shouldBeAtom("true", bool)
                it.thenBranch.shouldBeBlock { block ->
                    block.body[0].shouldBeAtom("2", intType)
                }
                it.elseBranch?.shouldBeBlock { block ->
                    block.body[0].shouldBeAtom("3", intType)
                }
            }
    }

    test("else branch should be optional") {
        ast("if(true) { 2 }")
            .shouldBeTypeOf<IfElse>().should {
                it.condition.shouldBeAtom("true", bool)
                it.thenBranch.shouldBeBlock { block ->
                    block.body[0].shouldBeAtom("2", intType)
                }
                it.elseBranch.shouldBeNull()
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

    test("should read field access") {
        ast(
            """
                data Foo = Bar(i: int)
                val baz = Bar(10)
                baz.i
            """.trimIndent()
        ).shouldBeTypeOf<FieldAccess>() should {
            it.receiver.type.shouldBeTypeOf<VariantType>() should { type ->
                type.name shouldBe "user/default.Foo"
                type.simpleName shouldBe "Foo"
            }
            it.fieldName shouldBe "i"
        }
    }

    test("should read field assignment") {
        ast(
            """
                data Foo = Bar(i: int)
                val baz = Bar(10)
                baz.i = 42
            """.trimIndent()
        ).shouldBeTypeOf<FieldAssignment>() should {
            it.receiver.type.shouldBeTypeOf<VariantType>() should { type ->
                type.simpleName shouldBe "Foo"
            }
            it.fieldName shouldBe "i"
            it.value.shouldBeAtom("42", intType)
        }
    }

    test("should read nested field assignment") {
        ast(
            """
                data Foo = Foo(i: int)
                data Bar = Bar(foo: Foo)
                data Baz = Baz(bar: Bar)
                val x = Baz(Bar(Foo(10)))
                x.bar.foo.i = 42
            """.trimIndent()
        ).shouldBeTypeOf<FieldAssignment>() should {
            it.fieldName shouldBe "i"
            it.value.shouldBeAtom("42", intType)

            it.receiver.shouldBeTypeOf<FieldAccess>() should { foo ->
                foo.fieldName shouldBe "foo"
                foo.type.name shouldBe "user/default.Foo"
                foo.receiver.shouldBeTypeOf<FieldAccess>() should { bar ->
                    bar.fieldName shouldBe "bar"
                    bar.type.name shouldBe "user/default.Bar"
                    bar.receiver.shouldBeTypeOf<VariableAccess>()
                }
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

//            .shouldBeTypeOf<FnCall>() should {
//            it.type.shouldBeTypeOf<VariantType>().should {
//                it.isGenericType().shouldBeTrue()
//                it.variant.shouldNotBeNull()
//                    .fields[0].type shouldBe intType
//            }
//        }
    }
})