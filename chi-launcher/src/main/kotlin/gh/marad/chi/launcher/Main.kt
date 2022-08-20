package gh.marad.chi.launcher

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import java.io.InputStreamReader

const val CHI = "chi"

fun main(args: Array<String>) {
    if (args.first() == "repl") {
        println(args)
        Repl(prepareContext(args.drop(1).toTypedArray(), emptyMap())).loop()
    } else {
        val options = mutableMapOf<String, String>()
        var file: String? = null
        val programArgs = mutableListOf<String>()
        args.forEach {
            if (!parseOption(options, it)) {
                if (file == null) {
                    file = it
                } else {
                    programArgs += it
                }
            }
        }

        val source = if (file != null) {
            Source.newBuilder(CHI, File(file!!)).build()
        } else {
            Source.newBuilder(CHI, InputStreamReader(System.`in`), "<stdin>").build()
        }

        val context = prepareContext(programArgs.toTypedArray(), options)
        context.eval(source)
        context.close()
    }
}

private fun prepareContext(args: Array<String>, options: Map<String, String>): Context =
    Context.newBuilder(CHI)
        .`in`(System.`in`)
        .out(System.out)
        .err(System.err)
        .arguments(CHI, args)
        .allowExperimentalOptions(true)
        .options(options)
        .build()

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