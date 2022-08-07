package gh.marad.chi.core.generics

import gh.marad.chi.ast
import gh.marad.chi.core.*
import gh.marad.chi.core.Type.Companion.array
import gh.marad.chi.core.Type.Companion.floatType
import gh.marad.chi.core.Type.Companion.genericFn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.string
import gh.marad.chi.core.Type.Companion.typeParameter
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class GenericsSpec : FunSpec({
    fun createScope() = CompilationScope().also {
        it.addSymbol(
            name = "array",
            type = genericFn(
                genericTypeParameters = listOf(typeParameter("T")),
                returnType = array(typeParameter("T")),
                intType,
                typeParameter("T")
            ),
            scope = SymbolScope.Package,
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
            messages shouldHaveSize 1
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
})