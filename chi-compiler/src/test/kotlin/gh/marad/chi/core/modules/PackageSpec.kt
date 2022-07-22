package gh.marad.chi.core.modules

import gh.marad.chi.core.Compiler
import io.kotest.core.spec.style.FunSpec

class PackageSpec : FunSpec({
    test("should set current module and package") {
        val source = """
            package my.module/some.system
            val millis = fn() { 0 }
        """.trimIndent()
        val result = Compiler.compile(source)

        result.messages.forEach { msg ->
            System.err.println(Compiler.formatCompilationMessage(source, msg))
        }

        assert(!result.hasErrors()) { "Compilation errors" }
    }

    test("allow using commas in module names") {
        TODO()
    }

    test("allow using commas in package names") {
        TODO()
    }
})