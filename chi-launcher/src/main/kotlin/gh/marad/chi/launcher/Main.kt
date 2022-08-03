package gh.marad.chi.launcher

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import java.io.InputStreamReader

const val CHI = "chi"

fun main(args: Array<String>) {
    if (args.first() == "repl") {
        Repl().loop()
    } else {
        val options = mutableMapOf<String, String>()
        var file: String? = null
        args.forEach {
            if (!parseOption(options, it)) {
                file = it
            }
        }

        val source = if (file != null) {
            Source.newBuilder(CHI, File(file!!)).build()
        } else {
            Source.newBuilder(CHI, InputStreamReader(System.`in`), "<stdin>").build()
        }

        executeSource(source, options)
    }
}

private fun executeSource(source: Source, options: Map<String, String>) {
    val context = Context.newBuilder(CHI)
        .`in`(System.`in`)
        .out(System.out)
        .err(System.err)
        .allowExperimentalOptions(true)
        .options(options)
        .build()

    context.eval(source)
    context.close()
}

private fun parseOption(options: MutableMap<String, String>, arg: String): Boolean {
    if (arg.length <= 2 || !arg.startsWith("--")) {
        return false
    }
    var (key, value) = if (arg.contains('=')) {
        arg.substring(2).split('=')
    } else {
        listOf(arg.substring(2), "true")
    }

    if (value.isEmpty()) {
        value = "true"
    }

    options[key] = value
    return true
}