package gh.marad.chi.core.modules

import gh.marad.chi.ast
import gh.marad.chi.core.FnCall
import gh.marad.chi.core.VariableAccess
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ImportSpec : FunSpec({
    // TODO:
    // - make parser understand the `import` syntax
    // - define `millis()` function in std/system - in compilation scope?

    test("using simplified name for names defined in current module") {
        // when
        val result = ast("""
            package user/default
            val foo = fn() { 1 }
            foo()
        """.trimIndent())

        // then
        result.shouldBeTypeOf<FnCall>().should { call ->
            call.function.shouldBeTypeOf<VariableAccess>().should {fn ->
                fn.moduleName shouldBe "user"
                fn.packageName shouldBe "default"
                fn.name shouldBe "foo"
            }
        }
    }

    test("importing function from package") {
        // given
        val result = ast("""
            import std/time { millis }
            millis()
        """.trimIndent(), ignoreCompilationErrors = true)

        // then
        result.shouldBeTypeOf<FnCall>().should { call ->
            call.function.shouldBeTypeOf<VariableAccess>().should {fn ->
                fn.moduleName shouldBe "std"
                fn.packageName shouldBe "time"
                fn.name shouldBe "millis"
            }
        }
    }

    test("import function with alias") {
        // given
        val result = ast("""
            import std/time { millis as coreMillis }
            coreMillis()
        """.trimIndent(), ignoreCompilationErrors = true)

        // then
        result.shouldBeTypeOf<FnCall>().should { call ->
            call.function.shouldBeTypeOf<VariableAccess>().should {fn ->
                fn.moduleName shouldBe "std"
                fn.packageName shouldBe "time"
                fn.name shouldBe "millis"
            }
        }
    }


    test("import whole package") {
//        Compiler.compile("""
//            import std/system
//            system.millis()
//        """.trimIndent())
        TODO()
    }

    test("import whole package with alias") {
//        Compiler.compile("""
//            import std/system as sys
//            sys.millis()
//        """.trimIndent())
        TODO()
    }

    test("import package and functions and alias everything") {
//        Compiler.compile("""
//            import std/system as sys { millis as coreMillis }
//            sys.millis()
//            coreMillis()
//        """.trimIndent())
        TODO()
    }

})
