package gh.marad.chi.core.sugar

import gh.marad.chi.ast
import gh.marad.chi.core.Fn
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf


class FunctionWithNameSpec : FunSpec({
    test("should define function with name") {
        val result = ast(
            """
            fn foo(a: int, b: bool): string {
              "hello world"
            }
        """.trimIndent()
        )

        result.shouldBeTypeOf<NameDeclaration>().should {
            it.name shouldBe "foo"
            it.immutable shouldBe true
            it.value.shouldBeTypeOf<Fn>().should { fn ->
                fn.returnType shouldBe Type.string
                fn.parameters.should { params ->
                    params[0].name shouldBe "a"
                    params[0].type shouldBe Type.intType
                    params[1].name shouldBe "b"
                    params[1].type shouldBe Type.bool
                }
            }
        }
    }

})