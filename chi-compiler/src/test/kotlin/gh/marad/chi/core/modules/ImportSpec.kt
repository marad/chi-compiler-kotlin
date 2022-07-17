package gh.marad.chi.core.modules

import gh.marad.chi.core.Compiler
import io.kotest.core.spec.style.FunSpec

class ImportSpec : FunSpec({
    // TODO:
    // - make parser understand the `import` syntax
    // - define `millis()` function in std/system - in compilation scope?

    test("using simplified name for names defined in current module") {
        Compiler.compile("""
            package user/default
            val foo = fn() { 1 }
            foo()
        """.trimIndent())
        TODO()
    }

    test("using fully qualified name") {
        Compiler.compile("""
            std/system.millis()
        """.trimIndent())
        TODO()
    }

    test("importing function from package") {
        Compiler.compile("""
            import std/system { millis }
            millis()
        """.trimIndent())
        TODO()
    }

    test("import whole package") {
        Compiler.compile("""
            import std/system
            system.millis()
        """.trimIndent())
        TODO()
    }

    test("import function with alias") {
        Compiler.compile("""
            import std/system { millis as coreMillis }
            coreMillis()
        """.trimIndent())
        TODO()
    }

    test("import whole package with alias") {
        Compiler.compile("""
            import std/system as sys
            sys.millis()
        """.trimIndent())
        TODO()
    }

    test("import package and functions and alias everything") {
        Compiler.compile("""
            import std/system as sys { millis as coreMillis }
            sys.millis()
            coreMillis()
        """.trimIndent())
        TODO()
    }

})
