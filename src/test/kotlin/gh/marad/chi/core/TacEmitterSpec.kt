package gh.marad.chi.core

import gh.marad.chi.tac.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TacEmitterSpec : FunSpec({
    test("should emit i32 value") {
        val result = emitTac("5")
        result shouldContainInOrder listOf(
            TacDeclaration("tmp$0", Type.i32, TacValue("5"))
        )
    }

    test("should emit f64 value") {
        val result = emitTac("5.4")
        result shouldHaveSingleElement
                TacDeclaration("tmp$0", Type.f64, TacValue("5.4"))
    }

    test("should emit symbol read") {
        val result = emitTac("x", mapOf("x" to Type.i32))
        result shouldContainInOrder listOf(
            TacDeclaration("tmp$0", Type.i32, TacName("x"))
        )
    }

    test("should emit value assignment") {
        val result = emitTac("val x = 5")
        result shouldHaveSize 1
        result[0] shouldBe TacDeclaration("x", Type.i32, TacValue("5"))
    }

    test("should emit assignment by name") {
        val result = emitTac("val y = x", mapOf("x" to Type.i32))
        result shouldHaveSize 1
        result[0] shouldBe TacDeclaration("y", Type.i32, TacName("x"))
    }

    test("should emit simple assignment") {
        val result = emitTac("x = 5", mapOf("x" to Type.i32))
        result shouldHaveSize 1
        result[0] shouldBe TacAssignment("x", Type.i32, TacValue("5"))
    }

    test("should emit complex assignment") {
        val result = emitTac("x = 2 + 5 * 3", mapOf("x" to Type.i32))
        result shouldHaveSize 6
        result[0] shouldBe TacDeclaration("tmp$0", Type.i32, TacValue("2"))
        result[1] shouldBe TacDeclaration("tmp$1", Type.i32, TacValue("5"))
        result[2] shouldBe TacDeclaration("tmp$2", Type.i32, TacValue("3"))
        result[3] shouldBe TacAssignmentOp("tmp$3", Type.i32, TacName("tmp$1"), "*", TacName("tmp$2"))
        result[4] shouldBe TacAssignmentOp("tmp$4", Type.i32, TacName("tmp$0"), "+", TacName("tmp$3"))
        result[5] shouldBe TacAssignment("x", Type.i32, TacName("tmp$4"))
    }

    test("should emit function") {
        val result = emitTac("fn() {}")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp$1", Type.fn(Type.unit), "tmp$0", emptyList(), emptyList())
    }

    test("should read function body") {
        val result = emitTac("fn() { val x = 5 }")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp$1", Type.fn(Type.unit), "tmp$0", emptyList(), listOf(
            TacDeclaration("x", Type.i32, TacValue("5"))
        ))
    }

    test("should add return if function expects a result") {
        val result = emitTac("fn(): i32 { val x = 5 }")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp$1", Type.fn(Type.i32), "tmp$0", emptyList(), listOf(
            TacDeclaration("x", Type.i32, TacValue("5")),
            TacReturn(Type.i32, TacName("x"))
        ))
    }

    test("should read function params params") {
        val result = emitTac("fn(a: i32, b: bool) {}")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp$1", Type.fn(Type.unit, Type.i32, Type.bool), "tmp$0", listOf("a", "b"), emptyList())
    }

    test("should emit function call and store result in temp variable") {
        val result = emitTac("inc(10)", mapOf("inc" to Type.fn(Type.i32, Type.i32)))
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp$0", Type.i32, TacValue("10"))
        result[1] shouldBe TacCall("tmp$1", Type.i32, "inc", listOf(TacName("tmp$0")))
    }

    test("should extract inner functions and substitute them with assignments") {
        val result = emitTac("val main = fn() { val inner = fn(){} }")
        result shouldHaveSize 2
        result[0] shouldBe TacFunction("tmp$0", Type.fn(Type.unit), "inner", emptyList(), emptyList())
        result[1] shouldBe TacFunction("tmp$2", Type.fn(Type.unit), "main", emptyList(), listOf(
            TacDeclaration("tmp$1", Type.fn(Type.unit), TacName("inner"))
        ))
    }

    test("should simulate if-else as an expression by using temp variable") {
        val result = emitTac("if (true) { 1 } else { 2 }")
        result shouldHaveSize 3
        result[0] shouldBe TacDeclaration("tmp$0", Type.bool, TacValue("true"))
        result[1] shouldBe TacDeclaration("tmp$1", Type.i32, null)
        result[2] shouldBe TacIfElse("tmp$1", Type.i32,
            condition = TacName("tmp$0"),
            thenBranch = listOf(
                TacDeclaration("tmp$2", Type.i32, TacValue("1")),
                TacAssignment("tmp$1", Type.i32, TacName("tmp$2")),
            ),
            elseBranch = listOf(
                TacDeclaration("tmp$3", Type.i32, TacValue("2")),
                TacAssignment("tmp$1", Type.i32, TacName("tmp$3")),
            ),
        )
    }

    test("should not require else branch and then if expression should have unit value") {
        val result = emitTac("if (true) { 1 }")
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp$0", Type.bool, TacValue("true"))
        result[1] shouldBe TacIfElse("tmp$1", Type.unit,
            condition = TacName("tmp$0"),
            thenBranch = listOf(
                TacDeclaration("tmp$2", Type.i32, TacValue("1")),
            ),
            elseBranch = null
        )
    }

    test("should emit not operator") {
        val result = emitTac("!true")
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp$0", Type.bool, TacValue("true"))
        result[1] shouldBe TacNot("tmp$1", TacName("tmp$0"))
    }

})

private fun emitTac(source: String, symbols: Map<String, Type> = emptyMap()): List<Tac> {
    val scope = CompilationScope().apply {
        symbols.map { addSymbol(it.key, it.value) }
    }

    val program = parseProgram(source, scope).first
    return TacEmitter().emitProgram(program)
}