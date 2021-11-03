package gh.marad.chi

import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize
import gh.marad.chi.interpreter.Scope

// Example code

// val x = 5
// val add = fn(a: i32, b: i32): i32 { a + b }
// val main = fn() {
//
// }
val code = """
    val x = 5
    val add = fn(a: i32, b: i32): i32 { a }
    val main = fn(): i32 {
       add(x, 3)
    }
""".trimIndent()


fun main() {
    val scope = Scope()
    while(true) {
        try {
            print("> ")
            val line = readLine() ?: continue
            if (line.isBlank()) continue
            val expressions = parse(tokenize(line))
            val result = expressions.map { scope.eval(it) }.last()
            println(result)
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}