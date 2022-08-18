package gh.marad.chi.core

import gh.marad.chi.ast
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ObjectsSpec : FunSpec({
    test("should find that member doesn't exist") {
        analyze(
            ast(
                """
                    data Test = Foo(i: int)
                    val x = Foo(10)
                    x.somethingElse
                """.trimIndent(), ignoreCompilationErrors = true
            )
        ).should { msgs ->
            msgs.shouldHaveSize(1)
            msgs[0].shouldBeTypeOf<MemberDoesNotExist>().should {
                it.level shouldBe Level.ERROR
                it.member shouldBe "somethingElse"
                it.type.name shouldBe "user/default.Test"
            }
        }
    }
})