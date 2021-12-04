package gh.marad.chi

import gh.marad.chi.core.Type
import gh.marad.chi.core.formatCompilationMessage
import gh.marad.chi.interpreter.Interpreter
import gh.marad.chi.interpreter.Value
import gh.marad.chi.interpreter.show
import gh.marad.chi.transpiler.transpile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString

class Tests : FunSpec({
    val testsFolder = "language-tests"
    val buildFolder = "build/language-tests"

    Files.createDirectories(Path.of(buildFolder))

    Files.list(Path.of(testsFolder)).forEach {
        val testName = it.name
        val mainCode = Files.readString(it.resolve("main.chi"))
        val expectedOutput = Files.readString(it.resolve("output.txt"))
            .fixNewlines()

        test("$testName - interpreted") {
            // given
            val interpreter = Interpreter()
            val output = StringBuilder()

            Type.primitiveTypes.forEach { type ->
                interpreter.registerNativeFunction("println", Type.fn(Type.unit, type)) { _, args ->
                    if (args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
                    output.appendLine(show(args.first()))
                    Value.unit
                }
            }

            // when
            val evalResult = interpreter.eval(mainCode)
            if (evalResult.messages.isNotEmpty()) {
                evalResult.messages.map { formatCompilationMessage(mainCode, it) }
                    .forEach {
                        println(it)
                    }
            }
            evalResult.messages shouldHaveSize 0
            interpreter.eval("main()")

            // then
            output.toString().fixNewlines() shouldBe expectedOutput
        }

        test("$testName - compiled") {
                val buildDir = Paths.get(buildFolder, testName)
                deleteDirectory(buildDir)
                Files.createDirectories(buildDir)

                val cCode = transpile(mainCode)
                val cFile = buildDir.resolve("test.c")
                val exeFile = buildDir.resolve("test.exe")

                Files.write(cFile, cCode.toByteArray())
                val gccOutput = "gcc ${cFile.fileName} -o${exeFile.fileName}".run(buildDir.toFile())
                println(gccOutput)
                val output = exeFile.pathString.run(buildDir.toFile())

                // then
                output.fixNewlines() shouldBe expectedOutput
        }
    }
})

fun String.run(workingDir: File): String {
    val process = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    val text = process.inputStream.bufferedReader().readText()
    process.waitFor(60, TimeUnit.MINUTES)
    println(process.exitValue())
    if (process.exitValue() != 0) {
        println(text)
        throw RuntimeException("Run failed")
    }
    return text
}

fun deleteDirectory(path: Path) {
    if (path.exists()) {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
    }
}

fun String.fixNewlines() = replace("\r\n", "\n").trim('\r', '\n', '\t', ' ')