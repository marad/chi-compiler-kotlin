package gh.marad.chi.launcher

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

const val CHI = "chi"

fun main(args: Array<String>) {
    if (args.first() == "repl") {
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

private fun prepareContext(args: Array<String>, options: Map<String, String>): Context {
    val context = Context.newBuilder(CHI)
        .`in`(System.`in`)
        .out(System.out)
        .err(System.err)
        .arguments(CHI, args)
        .allowExperimentalOptions(true)
        .allowAllAccess(true)
        .options(options)
        .build()

    println("Loading stdlib...")
    val start = System.currentTimeMillis()

    loadRecursively(context, Path.of("chi-stdlib"))
    println("Startup took ${System.currentTimeMillis() - start}ms")

    return context
}

private fun loadRecursively(context: Context, path: Path) {
    Files.list(path).forEach {
        if (it.isDirectory()) {
            loadRecursively(context, it)
        } else if (it.extension == "chi") {
            println(" - ${it.fileName}...")
            val source = Source.newBuilder("chi", it.toFile()).build()
            context.eval(source)
        }
    }
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