package gh.marad.chi.transpiler

import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.Type
import gh.marad.chi.core.formatCompilationMessage
import gh.marad.chi.utils.deleteDirectory
import gh.marad.chi.utils.runCommand
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    generateExecutable(listOf(Paths.get("language-tests/000-basic-test/main.chi")), Paths.get("compiled.exe"))
}

fun generateExecutable(chiFiles: List<Path>, targetExePath: Path) {
    val buildDir = Paths.get("build/chi")
    deleteDirectory(buildDir)
    Files.createDirectories(buildDir)

    val codeBuilder = StringBuilder()
    codeBuilder.appendLine("#include <stdio.h>")
    codeBuilder.appendLine("#include <stdbool.h>")
    codeBuilder.appendLine("#include <gc.h>")

    val compilationScope = CompilationScope()
    Prelude.init(compilationScope, codeBuilder)

    chiFiles.asSequence()
        .forEach { chiSourcePath ->
            val chiCode = Files.readString(chiSourcePath)
            val transpileResult = transpile(chiCode, compilationScope)

            transpileResult.messages.forEach {
                println(formatCompilationMessage(chiCode, it))
            }

            if (transpileResult.hasErrors()) {
                throw RuntimeException("Errors in ${chiSourcePath.toAbsolutePath()}")
            }

            codeBuilder.appendLine(transpileResult.cCode)
        }

    codeBuilder.appendLine("int main() { GC_init(); chi\$main(); return 0; }")

    val cCode = codeBuilder.toString()
    val cCodePath = buildDir.resolve("compiled.c")
    Files.write(cCodePath, cCode.toByteArray())

    arrayOf(
        "gcc",
        "-Iclibs/gc",
        "clibs/gc/gc.c",
        cCodePath.toAbsolutePath().toString(),
        "-o",
        targetExePath.toAbsolutePath().toString(),
    ).runCommand(File("."))
}

object Prelude {
    fun init(scope: CompilationScope, sb: StringBuilder) {
        scope.addSymbol("println", Type.fn(Type.unit, Type.i32))
        sb.append("""
            void println${'$'}i32(int i) {
              printf("%d\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.i64))
        sb.append("""
            void println${'$'}i64(long i) {
              printf("%d\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.f32))
        sb.append("""
            void println${'$'}f32(float i) {
              printf("%g\n", i);
            }
        """.trimIndent())

        scope.addSymbol("println", Type.fn(Type.unit, Type.f64))
        sb.append("""
            void println${'$'}f64(float i) {
              printf("%g\n", i);
            }
        """.trimIndent())
        sb.append('\n')
    }
}
