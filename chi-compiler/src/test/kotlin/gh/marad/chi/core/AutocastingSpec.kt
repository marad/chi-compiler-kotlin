package gh.marad.chi.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AutocastingSpec : FunSpec({
    test("should allow upcasting numbers") {
        Type.i32.canCastTo(Type.i64) shouldBe true
        Type.i32.canCastTo(Type.f32) shouldBe true
        Type.f32.canCastTo(Type.f64) shouldBe true
        Type.i32.canCastTo(Type.f64) shouldBe true
        Type.i64.canCastTo(Type.f64) shouldBe true
    }

    test("canCastTo should not allow downcasting") {
        Type.i64.canCastTo(Type.i32) shouldBe false
        Type.f64.canCastTo(Type.f32) shouldBe false
        Type.f64.canCastTo(Type.i64) shouldBe false
        Type.f64.canCastTo(Type.i32) shouldBe false
        Type.f32.canCastTo(Type.i32) shouldBe false
    }

    test("should allow downcasting numbers") {
        Type.i64.canDowncastTo(Type.i32) shouldBe true
        Type.f64.canDowncastTo(Type.f32) shouldBe true
        Type.f64.canDowncastTo(Type.i64) shouldBe true
        Type.f64.canDowncastTo(Type.i32) shouldBe true
        Type.f32.canDowncastTo(Type.i32) shouldBe true
    }
})