package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.core.Type.Companion.bool
import gh.marad.chi.core.Type.Companion.intType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class InfixOpsSpec : FreeSpec({
    "parser" - {
        "should read infix operations" {
            ast("1 + 2")
                .shouldBeTypeOf<InfixOp>().should {
                    it.op shouldBe "+"
                    it.left.shouldBeAtom("1", intType)
                    it.right.shouldBeAtom("2", intType)
                }
        }

        "should respect operator precedence" {
            ast("1 + 2 * 3")
                .shouldBeTypeOf<InfixOp>().should {
                    it.op shouldBe "+"
                    it.left.shouldBeAtom("1", intType)
                    it.right.shouldBeTypeOf<InfixOp>().should {inner ->
                        inner.op shouldBe "*"
                        inner.left.shouldBeAtom("2", intType)
                        inner.right.shouldBeAtom("3", intType)
                    }
                }
        }
    }

    "type checker" - {
        "should check that operation types match" {
            val result = analyze(ast("2 + true", ignoreCompilationErrors = true))

            result shouldHaveSize 1
            result.first().shouldBeTypeOf<TypeMismatch>().should {
                it.expected shouldBe intType
                it.actual shouldBe bool
            }
        }
    }

})