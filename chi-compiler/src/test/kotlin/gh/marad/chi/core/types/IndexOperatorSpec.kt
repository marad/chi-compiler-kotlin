package gh.marad.chi.core.types

import gh.marad.chi.core.Type
import gh.marad.chi.core.analyzer.TypeIsNotIndexable
import gh.marad.chi.core.analyzer.TypeMismatch
import gh.marad.chi.core.analyzer.analyze
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.expr
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class IndexOperatorSpec : FunSpec({
    val scope = CompilationScope(ScopeType.Package).also {
        it.addSymbol("arr", Type.array(Type.intType), SymbolType.Local)
    }

    test("should not allow to index arrays with types other than integer") {
        analyze(
            expr(
                """
                    arr["invalid-index"]
                """.trimIndent(), scope = scope
            )
        ).should { msgs ->
            msgs shouldHaveSize 1
            msgs.first().shouldBeTypeOf<TypeMismatch>().should {
                it.expected shouldBe Type.intType
                it.actual shouldBe Type.string
            }
        }
    }

    test("should not allow indexing arrays in assignment with non-integer types") {
        analyze(
            expr(
                """
                    arr["invalid-index"] = 5
                """.trimIndent(), scope = scope
            )
        ).should { msgs ->
            msgs shouldHaveSize 1
            msgs.first().shouldBeTypeOf<TypeMismatch>().should {
                it.expected shouldBe Type.intType
                it.actual shouldBe Type.string
            }
        }
    }

    test("should not allow indexing non-indexable types") {
        analyze(
            expr(
                """
                    5[2]
                """.trimIndent()
            )
        ).should { msgs ->
            msgs shouldHaveSize 1
            msgs[0].shouldBeTypeOf<TypeIsNotIndexable>().should {
                it.type shouldBe Type.intType
            }
        }
    }

    test("should not allow assign by index to non-indexable types") {
        analyze(
            expr(
                """
                    5[2] = 10
                """.trimIndent()
            )
        ).should { msgs ->
            msgs shouldHaveSize 1
            msgs[0].shouldBeTypeOf<TypeIsNotIndexable>().should {
                it.type shouldBe Type.intType
            }
        }
    }

    test("assigned value should match the element type") {
        analyze(
            expr(
                """
                    arr[2] = "i should be an int"
                """.trimIndent(), scope = scope
            )
        ).should { msgs ->
            msgs shouldHaveSize 1
            msgs[0].shouldBeTypeOf<TypeMismatch>().should {
                it.expected shouldBe Type.intType
                it.actual shouldBe Type.string
            }
        }
    }
})