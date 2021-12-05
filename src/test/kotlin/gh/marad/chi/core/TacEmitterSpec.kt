package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.f32
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.Type.Companion.i64
import gh.marad.chi.tac.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class TacEmitterSpec : FunSpec({
    test("should emit i32 value") {
        val result = emitTac("5")
        result shouldContainInOrder listOf(
            TacDeclaration("tmp_0", i32, TacValue("5"))
        )
    }

    test("should emit f64 value") {
        val result = emitTac("5.4")
        result shouldHaveSingleElement
                TacDeclaration("tmp_0", Type.f64, TacValue("5.4"))
    }

    test("should emit symbol read") {
        val result = emitTac("x", mapOf("x" to i32))
        result shouldContainInOrder listOf(
            TacDeclaration("tmp_0", i32, TacName("x"))
        )
    }

    test("should emit value assignment") {
        val result = emitTac("val x = 5")
        result shouldHaveSize 1
        result[0] shouldBe TacDeclaration("x", i32, TacValue("5"))
    }

    test("should emit assignment by name") {
        val result = emitTac("val y = x", mapOf("x" to i32))
        result shouldHaveSize 1
        result[0] shouldBe TacDeclaration("y", i32, TacName("x"))
    }

    test("should emit simple assignment") {
        val result = emitTac("x = 5", mapOf("x" to i32))
        result shouldHaveSize 1
        result[0] shouldBe TacAssignment("x", i32, TacValue("5"))
    }

    test("should emit complex assignment") {
        val result = emitTac("x = 2 + 5 * 3", mapOf("x" to i32))
        result shouldHaveSize 6
        result[0] shouldBe TacDeclaration("tmp_0", i32, TacValue("2"))
        result[1] shouldBe TacDeclaration("tmp_1", i32, TacValue("5"))
        result[2] shouldBe TacDeclaration("tmp_2", i32, TacValue("3"))
        result[3] shouldBe TacAssignmentOp("tmp_3", i32, TacName("tmp_1"), "*", TacName("tmp_2"))
        result[4] shouldBe TacAssignmentOp("tmp_4", i32, TacName("tmp_0"), "+", TacName("tmp_3"))
        result[5] shouldBe TacAssignment("x", i32, TacName("tmp_4"))
    }

    test("should emit complex assignment with proper auto-casted types") {
        val result = compile("""
            val a: i32 = 1
            val b: i64 = 2 as i64
            val c = a + b
        """.trimIndent())

        result.messages shouldBe emptyList()

        result.program.should {
            it[0] shouldBe TacDeclaration("a", i32, TacValue("1"))
            it[1] shouldBe TacDeclaration("tmp_0", i32, TacValue("2"))
            it[2] shouldBe TacCast("tmp_1", i64, TacName("tmp_0"))
            it[3] shouldBe TacDeclaration("b", i64, TacName("tmp_1"))
            it[4] shouldBe TacDeclaration("tmp_2", i32, TacName("a"))
            it[5] shouldBe TacCast("tmp_3", i64, TacName("tmp_2"))
            it[6] shouldBe TacDeclaration("tmp_4", i64, TacName("b"))
            it[7] shouldBe TacAssignmentOp("tmp_5", i64, TacName("tmp_3"), "+", TacName("tmp_4"))
            it[8] shouldBe TacDeclaration("c", i64, TacName("tmp_5"))
        }
    }

    test("should emit function") {
        val result = emitTac("fn() {}")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp_1", Type.fn(Type.unit), "tmp_0_", emptyList(), emptyList())
    }

    test("should read function body") {
        val result = emitTac("fn() { val x = 5 }")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp_1", Type.fn(Type.unit), "tmp_0_", emptyList(), listOf(
            TacDeclaration("x", i32, TacValue("5"))
        ))
    }

    test("should add return if function expects a result") {
        val result = emitTac("fn(): i32 { val x = 5 }")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp_1", Type.fn(i32), "tmp_0_", emptyList(), listOf(
            TacDeclaration("x", i32, TacValue("5")),
            TacReturn(i32, TacName("x"))
        ))
    }

    test("should read function params params") {
        val result = emitTac("fn(a: i32, b: bool) {}")
        result shouldHaveSize 1
        result[0] shouldBe TacFunction("tmp_1", Type.fn(Type.unit, i32, Type.bool), "tmp_0_i32_bool", listOf("a", "b"), emptyList())
    }

    test("should emit function call and store result in temp variable") {
        val result = emitTac("inc(10)", mapOf("inc" to Type.fn(i32, i32)))
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp_0", i32, TacValue("10"))
        result[1] shouldBe TacCall("tmp_1", i32, "inc_i32", listOf(TacName("tmp_0")))
    }

    test("should extract inner functions and substitute them with assignments") {
        val result = emitTac("val main = fn() { val inner = fn(){} }")
        result shouldHaveSize 2
        result[0] shouldBe TacFunction("tmp_0", Type.fn(Type.unit), "inner_", emptyList(), emptyList())
        result[1] shouldBe TacFunction("tmp_2", Type.fn(Type.unit), "main", emptyList(), listOf(
            TacDeclaration("tmp_1", Type.fn(Type.unit), TacName("inner_"))
        ))
    }

    test("should simulate if-else as an expression by using temp variable") {
        val result = emitTac("if (true) { 1 } else { 2 }")
        result shouldHaveSize 3
        result[0] shouldBe TacDeclaration("tmp_0", Type.bool, TacValue("true"))
        result[1] shouldBe TacDeclaration("tmp_1", i32, null)
        result[2] shouldBe TacIfElse("tmp_1", i32,
            condition = TacName("tmp_0"),
            thenBranch = listOf(
                TacDeclaration("tmp_2", i32, TacValue("1")),
                TacAssignment("tmp_1", i32, TacName("tmp_2")),
            ),
            elseBranch = listOf(
                TacDeclaration("tmp_3", i32, TacValue("2")),
                TacAssignment("tmp_1", i32, TacName("tmp_3")),
            ),
        )
    }

    test("should not require else branch and then if expression should have unit value") {
        val result = emitTac("if (true) { 1 }")
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp_0", Type.bool, TacValue("true"))
        result[1] shouldBe TacIfElse("tmp_1", Type.unit,
            condition = TacName("tmp_0"),
            thenBranch = listOf(
                TacDeclaration("tmp_2", i32, TacValue("1")),
            ),
            elseBranch = null
        )
    }

    test("should emit not operator") {
        val result = emitTac("!true")
        result shouldHaveSize 2
        result[0] shouldBe TacDeclaration("tmp_0", Type.bool, TacValue("true"))
        result[1] shouldBe TacNot("tmp_1", TacName("tmp_0"))
    }

    test("should add type suffixes to function definition names") {
        val result = compile("""
            val func = fn(a: i32): f32 { a as f32 }
            val func = fn(a: f32): i32 { a as i32 }
        """.trimIndent())

        result.program[0].shouldBeTypeOf<TacFunction>().functionName shouldBe "func_i32"
        result.program[1].shouldBeTypeOf<TacFunction>().functionName shouldBe "func_f32"
    }

    test("should add type suffixes to function calls") {
        val scope = CompilationScope()
        scope.addSymbol("x", i32)
        scope.addSymbol("f", Type.fn(f32, i32))
        val result = compile("f(x)", scope)

        result.program[1].shouldBeTypeOf<TacCall>().functionName shouldBe "f_i32"
    }

})

private fun emitTac(source: String, symbols: Map<String, Type> = emptyMap()): List<Tac> {
    val scope = CompilationScope().apply {
        symbols.map { addSymbol(it.key, it.value) }
    }

    val program = parseProgram(source, scope).first
    return TacEmitter().emitProgram(program)
}