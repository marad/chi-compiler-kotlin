package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.analyzer.TypeMismatch
import gh.marad.chi.core.analyzer.analyze
import gh.marad.chi.expr
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class InfixOpsSpec : FreeSpec({
    "type checker" - {
        "should check that operation types match" {
            val result = analyze(expr("2 + true"))

            result shouldHaveSize 1
            result.first().shouldBeTypeOf<TypeMismatch>().should {
                it.expected shouldBe intType
                it.actual shouldBe bool
            }
        }

        listOf("|", "&", "<<", ">>").forEach { op ->
            "should check that bit operator $op require ints" {
                analyze(expr("2 $op 2")).should { msgs ->
                    msgs shouldHaveSize 0
                }

                analyze(expr("true $op false")).should { msgs ->
                    msgs shouldHaveSize 1
                    msgs.first().shouldBeTypeOf<TypeMismatch>().should {
                        it.expected shouldBe intType
                        it.actual shouldBe bool
                    }
                }

                analyze(expr("true $op false")).should { msgs ->
                    msgs shouldHaveSize 1
                    msgs.first().shouldBeTypeOf<TypeMismatch>().should {
                        it.expected shouldBe intType
                        it.actual shouldBe bool
                    }
                }
            }
        }
    }

})