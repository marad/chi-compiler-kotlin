package gh.marad.chi.core

import gh.marad.chi.core.analyzer.Level
import gh.marad.chi.core.analyzer.MemberDoesNotExist
import gh.marad.chi.core.analyzer.TypeMismatch
import gh.marad.chi.core.analyzer.analyze
import gh.marad.chi.expr
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class ObjectsSpec : FunSpec({
    test("should find that member doesn't exist") {
        analyze(
            expr(
                """
                    data Test = Foo(i: int)
                    val x = Foo(10)
                    x.somethingElse
                """.trimIndent()
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

    test("check types for variant constructor invocation") {
        val msgs = analyze(
            expr(
                """
                    data Foo = Foo(i: int)
                    Foo("hello")
                """.trimIndent()
            )
        )

        msgs shouldHaveSize 1
        msgs[0].shouldBeTypeOf<TypeMismatch>() should {
            it.level shouldBe Level.ERROR
            it.expected shouldBe Type.intType
            it.actual shouldBe Type.string
        }
    }
})