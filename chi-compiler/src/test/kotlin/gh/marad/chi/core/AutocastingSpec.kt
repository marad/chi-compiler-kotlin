package gh.marad.chi.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AutocastingSpec : FunSpec({
    test("should allow upcasting numbers") {
        Type.intType.canCastTo(Type.floatType) shouldBe true
    }

    test("canCastTo should not allow downcasting") {
        Type.floatType.canCastTo(Type.intType) shouldBe false
    }

    test("should allow downcasting numbers") {
        Type.floatType.canDowncastTo(Type.intType) shouldBe true
    }
})