package gh.marad.chi.launcher

import org.graalvm.polyglot.Context

fun main() {
    val result = Context.create("chi").eval("chi", "val a = 23")
    println(result)
}