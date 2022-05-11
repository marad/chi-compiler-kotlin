package gh.marad.chi.launcher

import org.graalvm.polyglot.Context
import kotlin.system.exitProcess

fun main() {
    val context = Context.create("chi")
    while(true) {
        print("> ")
        val line = readLine()
        if (line == "exit") {
            exitProcess(0)
        }
        if (line.isNullOrEmpty()) {
            continue
        }
        try {
            val result = context.eval("chi", line)
            println(result)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}