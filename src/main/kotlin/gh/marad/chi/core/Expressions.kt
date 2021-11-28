package gh.marad.chi.core

import java.util.*

data class Location(val line: Int, val column: Int) {
    val formattedPosition = "$line:$column"
}

sealed interface Expression {
    val location: Location?
}

data class Program(val expressions: List<Expression>) : Expression {
    override val location: Location? = null
}

data class Atom(val value: String, val type: Type, override val location: Location?): Expression {
    companion object {
        fun unit(location: Location?) = Atom("()", Type.unit, location)
        fun i32(value: Int, location: Location?) = Atom("$value", Type.i32, location)
        fun f32(value: Float, location: Location?) = Atom("$value", Type.f32, location)
        fun bool(b: Boolean, location: Location?) = if (b) t(location) else f(location)
        fun t(location: Location?) = Atom("true", Type.bool, location)
        fun f(location: Location?) = Atom("false", Type.bool, location)
    }
}

data class VariableAccess(val enclosingScope: CompilationScope, val name: String, override val location: Location?): Expression

data class Assignment(val enclosingScope: CompilationScope, val name: String, val value: Expression, override val location: Location?) : Expression

data class NameDeclaration(val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?, override val location: Location?): Expression

data class FnParam(val name: String, val type: Type, val location: Location?)
data class Fn(val fnScope: CompilationScope, val parameters: List<FnParam>, val returnType: Type, val block: Block, override val location: Location?): Expression {
    val type = FnType(parameters.map { it.type }, returnType)
}
data class Block(val body: List<Expression>, override val location: Location?): Expression

data class FnCall(val enclosingScope: CompilationScope, val name: String, val parameters: List<Expression>, override val location: Location?): Expression

data class IfElse(val condition: Expression, val thenBranch: Block, val elseBranch: Block?, override val location: Location?) : Expression

data class InfixOp(val op: String, val left: Expression, val right: Expression, override val location: Location?) : Expression

data class PrefixOp(val op: String, val expr: Expression, override val location: Location?) : Expression

data class Cast(val expression: Expression, val targetType: Type, override val location: Location?) : Expression

data class CompilationScope(private val definedNames: MutableMap<String, Type> = mutableMapOf(),
                            private val parent: CompilationScope? = null) {

    fun addSymbol(name: String, type: Type) { definedNames[name] = type }
    fun getSymbol(name: String): Type? = definedNames[name] ?: parent?.getSymbol(name)
}
