package gh.marad.chi.core.generics

import gh.marad.chi.ast
import gh.marad.chi.asts
import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.array
import gh.marad.chi.core.Type.Companion.floatType
import gh.marad.chi.core.Type.Companion.genericFn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.string
import gh.marad.chi.core.Type.Companion.typeParameter
import gh.marad.chi.core.analyzer.GenericTypeArityError
import gh.marad.chi.core.analyzer.Level
import gh.marad.chi.core.analyzer.TypeMismatch
import gh.marad.chi.core.analyzer.analyze
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class GenericsSpec : FunSpec({
    fun createScope() = CompilationScope(ScopeType.Package).also {
        it.addSymbol(
            name = "array",
            type = genericFn(
                genericTypeParameters = listOf(typeParameter("T")),
                returnType = array(typeParameter("T")),
                intType,
                typeParameter("T")
            ),
            scope = SymbolType.Local,
        )
    }

    test("should read generic element type for array") {
        forAll(
            table(
                headers("expectedType", "defaultValue"),
                row(intType, "0"),
                row(string, "\"hello\""),
                row(floatType, "0.0"),
            )
        ) { expectedType, defaultValue ->
            ast(
                """
                    val a = array[${expectedType.name}](10, $defaultValue)
                    a
                """.trimIndent(), createScope()
            ).shouldBeTypeOf<VariableAccess>().should {
                it.type shouldBe array(expectedType)
            }
        }

    }


    test("should check that generic type provided matches the type of the argument") {
        analyze(
            ast(
                """
                    array[int](10, "im a string")
                """.trimIndent(), createScope(), ignoreCompilationErrors = true
            )
        ).should { messages ->
//            messages shouldHaveSize 1
            messages[0].shouldBeTypeOf<TypeMismatch>().should {
                it.actual shouldBe string
                it.expected shouldBe intType
            }
        }
    }

    test("generic function call should have correct amount of type parameters") {
        analyze(
            ast(
                """
                    array[int, string](10, 0)
                """.trimIndent(), createScope(), ignoreCompilationErrors = true
            )
        ).should { messages ->
            messages shouldHaveSize 1
            messages[0].shouldBeTypeOf<GenericTypeArityError>().should {
                it.expectedCount shouldBe 1
                it.actualCount shouldBe 2
            }
        }
    }

    test("can define a generic function") {
        asts(
            """
                fn myfunc[T](param: T): T { param }
                val i = myfunc[int](5)
                val s = myfunc[string]("hello")
            """.trimIndent()
        ).should { exprs ->
            exprs[1].shouldBeTypeOf<NameDeclaration>().should {
                it.value.type shouldBe intType
            }
            exprs[2].shouldBeTypeOf<NameDeclaration>().should {
                it.value.type shouldBe string
            }
        }
    }

    test("assignment should check generic type for composite type") {
        // when
        val msgs = analyze(
            ast(
                """
                    data Foo[T] = Foo(t: T)
                    val x: Foo[int] = Foo("hello")
                """.trimIndent(), ignoreCompilationErrors = true
            )
        )

        // then
        msgs shouldHaveSize 1
        msgs[0].shouldBeTypeOf<TypeMismatch>() should {
            it.level shouldBe Level.ERROR
            it.expected.shouldBeTypeOf<VariantType>() should { expectedType ->
                expectedType.name shouldBe "user/default.Foo"
                expectedType.concreteTypeParameters[typeParameter("T")] shouldBe intType
            }

            it.actual.shouldBeTypeOf<VariantType>() should { actualType ->
                actualType.name shouldBe "user/default.Foo"
                actualType.concreteTypeParameters[typeParameter("T")] shouldBe string
            }
        }
    }

    test("type matching works") {
        // when
        val result = matchCallTypes(
            listOf(array(array(typeParameter("T")))),
            listOf(array(array(intType)))
        )

        // then
        result[typeParameter("T")] shouldBe intType
    }

    test("should check concrete return values") {
        analyze(
            ast(
                """
                    fn f[T](param: T): T { "hello" }
                """.trimIndent(), ignoreCompilationErrors = true
            )
        ) should { msgs ->
            msgs shouldHaveSize 1
            msgs[0].shouldBeTypeOf<TypeMismatch>() should {
                it.actual shouldBe string
                it.expected shouldBe typeParameter("T")
            }
        }
    }

    test("check more complex use case") {
        ast(
            """
                fn f[T](arr: array[T], index: int): T { arr[index] }
                val arr = array(10, 0)
                arr[0] = 10
                f(arr, 0)
            """.trimIndent(), scope = createScope()
        ).shouldBeTypeOf<FnCall>() should { fnCall ->
            fnCall.type shouldBe intType
        }
    }

    test("generic type parameters are more important than parameter types") {
        val messages = analyze(
            ast(
                """
                    fn f[T](param: T): T { param }
                    f[any](5)
                """.trimIndent(), ignoreCompilationErrors = true
            )
        )

        messages.shouldBeEmpty()
    }

    test("concrete type parameters should be used in parameter type checking") {
        // given
        val expression = ast(
            """
            data HashMap[K,V] = HashMap(impl: any)
            fn hashMap[K,V](): HashMap[K,V] {
                HashMap[K,V](0)
            }
            fn assoc[A,B](m: HashMap[A,B], key: A, value: B) {}
            
            val m = hashMap[string, int]()
            assoc(m, "hello", "world")
        """.trimIndent(), ignoreCompilationErrors = true
        )

        // when
        val messages = analyze(expression)

        // then
        messages shouldHaveSize 1
        messages[0].shouldBeTypeOf<TypeMismatch>() should {
            it.expected.shouldBeTypeOf<VariantType>() should { expectedType ->
                expectedType.concreteTypeParameters shouldBe mapOf(
                    typeParameter("K") to string,
                    typeParameter("V") to string

                )
            }

            it.actual.shouldBeTypeOf<VariantType>() should { actualType ->
                actualType.concreteTypeParameters shouldBe mapOf(
                    typeParameter("K") to string,
                    typeParameter("V") to intType
                )
            }

        }
    }

    test("complex type checking example") {
        val scope = createScope().apply {
            addSymbol(
                "unsafeArray",
                genericFn(
                    listOf(typeParameter("T")),
                    array(typeParameter("T")),
                    intType,
                ),
                SymbolType.Local
            )
            addSymbol(
                "size",
                genericFn(
                    listOf(typeParameter("T")),
                    intType,
                    array(typeParameter("T")),
                ),
                SymbolType.Local
            )
        }
        analyze(
            ast(
                """
                    fn map[T, R](arr: array[T], f: (T) -> R): array[R] {
                        val arrSize = size(arr)
                        val result = unsafeArray[R](arrSize)
                        var index = 0
                        while(index < arrSize) {
                            val tmp = f(arr[index])
                            result[index] = tmp
                            index = index + 1
                        }
                        result
                    }
                """.trimIndent(), scope = scope, ignoreCompilationErrors = false
            )
        )
    }

    test("is type conversion smoke test") {
        val code = """
            data A = A(a: int)
            data B[T] = B(b: T)
            val x = B(A(10))
            fn foo[T](x: B[T]): int {
                val a = x.b
                if (a is A) {
                    a.a
                } else {
                    0
                }
            }
        """.trimIndent()
        ast(code)
    }

//  I'm not even sure how this should be handled. Maybe generic/any functions should simply override each other?
//    test("defining overloaded functions with generic and any types collide") {
//        val messages = analyze(
//            ast(
//                """
//                    fn foo(param: any) {}
//                    fn foo[T](t: T) {}
//                """.trimIndent(), ignoreCompilationErrors = true
//            )
//        )
//
//        messages shouldHaveSize 1
//    }
})