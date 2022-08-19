package gh.marad.chi.launcher

import gh.marad.chi.truffle.ChiLanguage
import org.graalvm.polyglot.Context


class Repl {
    private val context = Context.create(ChiLanguage.id)
    private var imports = ""
    private var shouldContinue = true

    fun loop() {
        while (true) {
            try {
                step()
                if (!shouldContinue) break
            } catch (ex: Exception) {
                val sb = StringBuilder()
                ex.stackTrace.forEach {
                    if (it.className.startsWith("gh.marad.chi")) {
                        sb.appendLine(it)
                    }
                }
                System.err.println(sb.toString())
            }
        }
    }

    private fun step() {
        print("> ")
        val input = readln().trim()
        if (input == "exit") {
            exit()
        } else if (input.startsWith("import ")) {
            recordImport(input)
        } else {
            val result = context.eval(ChiLanguage.id, prepareSource(input))
            println(result.toString())
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