package gh.marad.chi.core

import gh.marad.chi.actionast.makeIfAnExpression
import gh.marad.chi.ast
import gh.marad.chi.asts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder

class SimplifierSpec : FunSpec() {
    init {
        test("should make `if` an expression-like") {
            val input = ast("""
                if (1) { 2 } else { 3 }
            """.trimIndent())

            val expectedOutput = asts("""
                var tmp = 0
                if (1) { tmp = 2 } else { tmp = 3 }
                tmp
            """.trimIndent())


            makeIfAnExpression("tmp", input as IfElse).shouldContainInOrder(expectedOutput)
        }
    }
}