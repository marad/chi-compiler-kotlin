package gh.marad.chi.launcher

import gh.marad.chi.truffle.ChiLanguage
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException


class Repl(private val context: Context) {
    private var imports = ""
    private var shouldContinue = true

    fun loop() {
        while (true) {
            try {
                step()
                if (!shouldContinue) break
            } catch (ex: PolyglotException) {
                if (ex.message?.contains("Compilation failed") != true) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun step() {
        print("> ")
        val input = readInputLine()
        if (input == "exit") {
            exit()
        } else if (input.startsWith("import ")) {
            recordImport(input)
        } else {
            val result = context.eval(ChiLanguage.id, prepareSource(input))
            println(result.toString())
        }
    }

    private fun readInputLine(): String {
        return if (System.console() != null) {
            System.console().readLine()
        } else {
            readln()
        }
    }

    private fun recordImport(input: String) {
        imports += "$input\n"
    }

    private fun prepareSource(input: String): String = "$imports\n$input".trim()

    private fun exit() {
        shouldContinue = false
    }
}