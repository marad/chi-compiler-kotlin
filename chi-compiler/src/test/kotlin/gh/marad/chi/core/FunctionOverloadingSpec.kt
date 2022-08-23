package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.any
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.Type.Companion.string
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FunctionOverloadingSpec : FunSpec({

    test("should choose function type with correct parameter types") {
        val overloadedFnType = OverloadedFnType(
            setOf(
                fn(intType, intType),
                fn(intType, string)
            )
        )

        overloadedFnType.getType(listOf(intType)) shouldBe fn(intType, intType)
        overloadedFnType.getType(listOf(string)) shouldBe fn(intType, string)
    }

    test("should match parameter count and order") {
        val overloadedFnType = OverloadedFnType(
            setOf(
                fn(intType, intType),
                fn(intType, intType, string),
                fn(intType, string, intType)
            )
        )


        overloadedFnType.getType(listOf(intType)) shouldBe fn(intType, intType)
        overloadedFnType.getType(listOf(intType, string)) shouldBe fn(intType, intType, string)
        overloadedFnType.getType(listOf(string, intType)) shouldBe fn(intType, string, intType)
    }

    test("should prefer more concrete types over any") {
        val overloadedFnType = OverloadedFnType(setOf(fn(intType, any), fn(intType, intType), fn(intType, string)))
        overloadedFnType.getType(listOf(intType)) shouldBe fn(intType, intType)
    }

    test("should not find type if decision is not certain") {
        val overloadedFnType = OverloadedFnType(
            setOf(
                fn(intType, any, intType),
                fn(intType, intType, any),
            )
        )

        overloadedFnType.getType(listOf(intType, intType)).shouldBeNull()
    }
})