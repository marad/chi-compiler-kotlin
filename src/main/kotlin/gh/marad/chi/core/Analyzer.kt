package gh.marad.chi.core

data class Context(val dummy: Int)
enum class Level { WARNING, ERROR }
data class Message(val level: Level, val message: String)

fun analyze(context: Context, expression: Expression): List<Message> {
    return emptyList()
}