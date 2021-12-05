package gh.marad.chi

import gh.marad.chi.core.Type
import gh.marad.chi.core.formatCompilationMessage
import gh.marad.chi.interpreter.Interpreter
import gh.marad.chi.interpreter.Value
import gh.marad.chi.interpreter.show
import gh.marad.chi.transpiler.generateExecutable
import gh.marad.chi.utils.deleteDirectory
import gh.marad.chi.utils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.pathString

class Tests : FunSpec({
    val testsFolder = "language-tests"
    val buildFolder = "build/language-tests"

    Files.createDirectories(Path.of(buildFolder))

    Files.list(Path.of(testsFolder)).forEach { testFolder ->
        val testName = testFolder.name
        val mainCode = Files.readString(testFolder.resolve("main.chi"))
        val expectedOutput = Files.readString(testFolder.resolve("output.txt"))
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

            val exeFile = buildDir.resolve("test.exe")
            generateExecutable(listOf(testFolder.resolve("main.chi")), exeFile)

            val output = exeFile.pathString.runCommand(buildDir.toFile())

            // then
            output.fixNewlines() shouldBe expectedOutput
        }
    }
})

fun String.fixNewlines() = replace("\r\n", "\n").trim('\r', '\n', '\t', ' ')