@file:Suppress("unused")

package gh.marad.chi.core.analyzer

import gh.marad.chi.*
import gh.marad.chi.core.Block
import gh.marad.chi.core.Type
import gh.marad.chi.core.Type.Companion.array
import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.floatType
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.string
import gh.marad.chi.core.Type.Companion.typeParameter
import gh.marad.chi.core.Type.Companion.unit
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class AssignmentTypeCheckingSpec : FunSpec() {
    init {
        test("should check that type of the variable matches type of the expression") {
            val scope = CompilationScope(ScopeType.Package)
            scope.addSymbol("x", intType, SymbolType.Local, mutable = true)
            analyze(expr("x = 10", scope)).shouldBeEmpty()
            analyze(expr("x = {}", scope)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }
        }

        test("should prohibit changing immutable values") {
            analyze(
                expr(
                    """
                        val x = 10
                        x = 15
                    """.trimIndent()
                )
            ).should { msgs ->
                msgs shouldHaveSize 1
                msgs[0].shouldBeTypeOf<CannotChangeImmutableVariable>().should {
                    it.level shouldBe Level.ERROR
                }
            }
        }
    }
}

class NameDeclarationTypeCheckingSpec : FunSpec() {
    init {

        test("should return nothing for simple atom and variable read") {
            val scope = CompilationScope(ScopeType.Package)
            scope.addSymbol("x", Type.fn(unit), SymbolType.Local)
            analyze(expr("5", scope)).shouldBeEmpty()
            analyze(expr("x", scope)).shouldBeEmpty()
        }

        test("should check if types match in name declaration with type definition") {
            analyze(expr("val x: () -> int = 5")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe Type.fn(intType)
                    error.actual shouldBe intType
                }
            }
        }

        test("should pass valid name declarations") {
            analyze(expr("val x: int = 5")).shouldBeEmpty()
            analyze(expr("val x = 5")).shouldBeEmpty()
        }
    }
}

class BlockExpressionTypeCheckingSpec : FunSpec() {
    init {
        test("should check contained expressions") {
            val block = Block(
                expressions(
                    """
                        val x: () -> int = 10
                        fn foo(): int {}
                    """.trimIndent()
                ),
                null
            )

            val errors = analyze(block)
            errors.shouldHaveSize(2)
            errors.should {
                errors[0].shouldBeTypeOf<MissingReturnValue>().should { error ->
                    error.expectedType shouldBe intType
                }
                errors[1].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe Type.fn(intType)
                    error.actual shouldBe intType
                }
            }
        }
    }
}

class FnTypeCheckingSpec : FunSpec() {
    init {
        test("should not return errors on valid function definition") {
            analyze(expr("{ x: int -> x }")).shouldBeEmpty()
            analyze(expr("fn foo(x: int): int { x }")).shouldBeEmpty()
        }

        test("should check for missing return value only if function expects the return type") {
            analyze(expr("fn foo() {}"))
                .shouldBeEmpty()
            analyze(expr("fn foo(): int {}")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<MissingReturnValue>().should { error ->
                    error.expectedType shouldBe intType
                }
            }
        }

        test("should check that block return type matches what function expects") {
            analyze(expr("fn foo(): int { {} }")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }

            // should point to '{' of the block when it's empty instead of last expression
            analyze(expr("fn foo(): int {}")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<MissingReturnValue>().should { error ->
                    error.expectedType shouldBe intType
                }
            }
        }

        test("should also check types for expressions in function body") {
            analyze(
                expr(
                    """
                        fn foo(x: int): int {
                            val i: int = {}
                            x
                        }
                    """.trimIndent()
                )
            ).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe intType
                    error.actual shouldBe Type.fn(unit)
                }
            }
        }
    }
}

class FnCallTypeCheckingSpec : FunSpec() {
    init {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("x", intType, SymbolType.Local)
        scope.addSymbol("test", Type.fn(intType, intType, Type.fn(unit)), SymbolType.Local)

        test("should check that parameter argument types match") {
            analyze(expr("test(10, {})", scope)).shouldBeEmpty()
            analyze(expr("test(10, 20)", scope)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe Type.fn(unit)
                    error.actual shouldBe intType
                }
            }
        }

        test("should check function arity") {
            analyze(expr("test(1)", scope)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<FunctionArityError>().should { error ->
                    error.expectedCount shouldBe 2
                    error.actualCount shouldBe 1
                }
            }
        }

        test("should check that only functions are called") {
            analyze(expr("x()", scope)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<NotAFunction>()
            }
        }

        test("should check that proper overloaded function exists") {
            val localScope = CompilationScope(ScopeType.Package)
            localScope.addSymbol("test", Type.fn(intType, intType), SymbolType.Local)
            localScope.addSymbol("test", Type.fn(intType, floatType), SymbolType.Local)

            analyze(expr("test(2)", localScope)).shouldBeEmpty()
            analyze(expr("test(2 as unit)", localScope)).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<NoCandidatesForFunction>().should { error ->
                    error.argumentTypes shouldBe listOf(unit)
                }
            }
        }

        test("should resolve generic return type for complex case") {
            // given
            val localScope = CompilationScope(ScopeType.Package)
            localScope.addSymbol(
                "map", Type.genericFn(
                    listOf(typeParameter("T"), typeParameter("R")),
                    array(typeParameter("R")),
                    array(typeParameter("T")),
                    Type.fn(typeParameter("R"), typeParameter("T"))
                ),
                SymbolType.Local
            )
            localScope.addSymbol("operation", Type.fn(string, intType), SymbolType.Local)
            localScope.addSymbol("arr", array(intType), SymbolType.Local)

            // when
            val result = expr("map(arr, operation)", scope = localScope)

            // then
            result.type shouldBe array(string)
        }

        test("should check explicitly specified call type params") {
            // given
            val localScope = CompilationScope(ScopeType.Package)
            localScope.addSymbol(
                "map", Type.genericFn(
                    listOf(typeParameter("T"), typeParameter("R")),
                    array(typeParameter("R")),
                    array(typeParameter("T")),
                    Type.fn(typeParameter("R"), typeParameter("T"))
                ),
                SymbolType.Local
            )
            localScope.addSymbol("operation", Type.fn(unit, intType), SymbolType.Local)
            localScope.addSymbol("arr", array(intType), SymbolType.Local)

            // when
            val messages =
                analyze(expr("map[int, string](arr, operation)", scope = localScope))

            // then
            messages shouldHaveSize 1
            messages[0].shouldBeTypeOf<GenericTypeMismatch>() should {
                it.expected shouldBe string
                it.actual shouldBe unit
                it.genericTypeParameter shouldBe typeParameter("R")
            }
        }

        test("should check explicitly specified type parameter when it's only used as return value") {
            val localScope = CompilationScope(ScopeType.Package).apply {
                addSymbol(
                    "unsafeArray",
                    Type.genericFn(
                        listOf(typeParameter("T")),
                        array(typeParameter("T")),
                        intType,
                    ),
                    SymbolType.Local
                )
            }

            val result = expr("unsafeArray[int](10)", scope = localScope)

            result.type shouldBe array(intType)
        }

        test("constructing recurring generic data type should work") {
            expr(
                """
                    data List[T] = Node(head: T, tail: List[T]) | Nil
                    Node(10, Node(20, Nil))
                """.trimIndent()
            )
        }


        test("typechecking should work for generic parameter types in type constructors") {
            val result = analyze(
                expr(
                    """
                    data List[T] = Node(head: T, tail: List[T]) | Nil
                    Node(10, Node("string", Nil))
                """.trimIndent()
                )
            )

            result.shouldNotBeEmpty()
            result[0].shouldBeTypeOf<TypeMismatch>() should {
                it.expected shouldBe string
                it.actual shouldBe intType
            }
        }

        test("should check types for chain function calls") {
            val result = analyze(
                expr(
                    """
                        val foo = { a: int ->
                        	{ 42 }
                        }
                        foo()()
                    """.trimIndent()
                )
            )

            result shouldHaveSize 1
            result[0].shouldBeTypeOf<FunctionArityError>() should {
                it.expectedCount shouldBe 1
                it.actualCount shouldBe 0
            }
        }

        test("should accept function returning non-unit value when unit-returning function is expected as an argument") {
            val code = """
                fn forEach(f: (string) -> unit) { }    
                forEach({ it: string -> it })
            """.trimIndent()

            val result = analyze(expr(code))

            result.shouldBeEmpty()
        }
    }
}

class IfElseTypeCheckingSpec : FunSpec() {
    init {
        test("if-else type is unit when branch types differ (or 'else' branch is missing)") {
            analyze(expr("val x: unit = if(true) { 2 }")).shouldBeEmpty()
            analyze(expr("val x: int = if(true) { 2 } else { 3 }")).shouldBeEmpty()
            analyze(expr("val x: int = if(true) { 2 } else { {} }")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe intType
                    error.actual shouldBe unit
                }
            }
        }

        test("conditions should be boolean type") {
            analyze(expr("if (1) { 2 }")).should {
                it.shouldHaveSize(1)
                it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                    error.expected shouldBe bool
                    error.actual shouldBe intType
                }
            }
        }
    }
}

class PrefixOpSpec : FunSpec({
    test("should expect boolean type for '!' operator") {
        analyze(expr("!true")) shouldHaveSize 0
        analyze(expr("!1")).should {
            it.shouldHaveSize(1)
            it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                error.expected shouldBe bool
                error.actual shouldBe intType
            }
        }
    }
})

class CastSpec : FunSpec({
})

class WhileLoopSpec : FunSpec({
    test("condition should have boolean type") {
        analyze(expr("while(true) {}")) shouldHaveSize 0
        analyze(expr("while(1) {}")).should {
            it.shouldHaveSize(1)
            it[0].shouldBeTypeOf<TypeMismatch>().should { error ->
                error.expected shouldBe bool
                error.actual shouldBe intType
            }
        }
    }
})

class IsExprSpec : FunSpec({
    test("is expr should cooperate with if providing a scope") {
        val code = """
            data AB = A(a: int) | B(b: float)
            val a = A(10)
            if (a is B) {
                a.b
            }
        """.trimIndent()

        val errors = analyze(expr(code))

        errors.shouldBeEmpty()
    }

    test("is expr should fill variant within `when` branches") {
        val code = """
            data AB = A(a: int) | B(b: int) | C(c: int) | D(d: int)
            val x = A(10)
            when {
                x is A -> x.a
                x is B -> x.b
                x is C -> x.c
                x is D -> x.d
            }
        """.trimIndent()

        val errors = analyze(expr(code))

        errors.shouldBeEmpty()
    }

    test("should also work with imported types") {
        val namespace = GlobalCompilationNamespace()
        val defCode = """
            package foo/bar
            data AB = pub A(a: int) | B(pub b: float)
        """.trimIndent()
        compile(defCode, namespace)

        val code = """
            import foo/bar { AB }
            val a = A(10)
            if (a is B) {
                a.b
            }
        """.trimIndent()
        compile(code, namespace)
    }

    test("should not allow importing variables and functions that are not public") {
        val namespace = GlobalCompilationNamespace()
        val defCode = """
            package mymod/mypkg
            fn foo() { 0 }
            val bar = 0
            pub fn baz() { 0 }
            pub val faz = 0
        """.trimIndent()
        compile(defCode, namespace)

        val code = """
            import mymod/mypkg { foo, bar, baz, faz }
        """.trimIndent()
        val result = analyze(expressions(code, namespace)[0])

        result shouldHaveSize 2
        result[0].shouldBeTypeOf<ImportInternal>()
            .symbolName shouldBe "foo"
        result[1].shouldBeTypeOf<ImportInternal>()
            .symbolName shouldBe "bar"
    }
})