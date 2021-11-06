package gh.marad.chi.core


sealed interface Expression {
    val location: Location?
}
data class Atom(val value: String, val type: Type, override val location: Location?): Expression {
    companion object {
        fun unit(location: Location?) = Atom("()", Type.unit, location)
    }
}

data class VariableAccess(val name: String, override val location: Location?): Expression

data class Assignment(val name: String, val value: Expression, override val location: Location?) : Expression

data class NameDeclaration(val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?, override val location: Location?): Expression

data class FnParam(val name: String, val type: Type, val location: Location?)
data class Fn(val parameters: List<FnParam>, val returnType: Type, val block: Block, override val location: Location?): Expression {
    val type = FnType(parameters.map { it.type }, returnType)
}
data class Block(val body: List<Expression>, override val location: Location?): Expression

data class FnCall(val name: String, val parameters: List<Expression>, override val location: Location?): Expression

data class IfElse(val condition: Expression, val thenBranch: Block, val elseBranch: Block?, override val location: Location?) : Expression

