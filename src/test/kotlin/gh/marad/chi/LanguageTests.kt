package gh.marad.chi

import gh.marad.chi.core.Type
import gh.marad.chi.interpreter.Interpreter
import gh.marad.chi.interpreter.Value
import gh.marad.chi.interpreter.show
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

class LanguageTests : FreeSpec({
    val testsFolder = "language-tests"
    Files.list(Path.of(testsFolder)).forEach {
        it.name - {
            val mainCode = Files.readString(it.resolve("main.chi"))
            val expectedOutput = Files.readString(it.resolve("output.txt"))

            "interpreted" {
                // given
                val interpreter = Interpreter()
                val output = StringBuilder()

                interpreter.registerNativeFunction("println", Type.fn(Type.unit, Type.i32)) { _, args ->
                    if (args.size != 1) throw RuntimeException("Expected one argument got ${args.size}")
                    output.appendLine(show(args.first()))
                    Value.unit
                }

                // when
                interpreter.eval(mainCode)
                interpreter.eval("main()")

                // then
                output.toString() shouldBe expectedOutput
            }

            "compiled" {
                TODO()
            }
        }
    }
})