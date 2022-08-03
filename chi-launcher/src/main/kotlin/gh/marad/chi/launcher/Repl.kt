package gh.marad.chi.launcher

import gh.marad.chi.truffle.ChiLanguage
import org.graalvm.polyglot.Context


class Repl {
    val context = Context.create(ChiLanguage.id)

    fun loop() {
        while (true) {
            try {
                if (!step()) break
            } catch (ex: Exception) {
                println(ex.message)
            }
        }
    }

    private fun step(): Boolean {
        print("> ")
        val input = readln().trim()
        if (input == "exit") {
            return false
        }

        val result = context.eval(ChiLanguage.id, input)
        println(result.toString())
        return true
    }
}