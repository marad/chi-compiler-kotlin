package gh.marad.chi.core

data class Location(val line: Int, val column: Int) {
    val formattedPosition = "$line:$column"
}

sealed interface Expression {
    val location: Location?
    val type: Type
}

data class Program(val expressions: List<Expression>) : Expression {
    override val location: Location? = null
    override val type: Type = expressions.lastOrNull()?.type ?: Type.unit
}

data class Atom(val value: String, override val type: Type, override val location: Location?): Expression {
    companion object {
        fun unit(location: Location?) = Atom("()", Type.unit, location)
        fun int(value: Int, location: Location?) = Atom("$value", Type.intType, location)
        fun float(value: Float, location: Location?) = Atom("$value", Type.floatType, location)
        fun bool(b: Boolean, location: Location?) = if (b) t(location) else f(location)
        fun t(location: Location?) = Atom("true", Type.bool, location)
        fun f(location: Location?) = Atom("false", Type.bool, location)
        fun string(value: String, location: Location?) = Atom(value, Type.string, location)
    }
}

data class VariableAccess(val enclosingScope: CompilationScope, val name: String,
                          override val location: Location?): Expression {
    override val type: Type
        get() = enclosingScope.getSymbol(name) ?: Type.undefined
}

data class Assignment(val enclosingScope: CompilationScope, val name: String, val value: Expression,
                      override val location: Location?) : Expression {
    override val type: Type = value.type
}

data class NameDeclaration(val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?, override val location: Location?): Expression {
    override val type: Type = expectedType ?: value.type
}

data class Group(val value: Expression, override val location: Location?): Expression {
    override val type: Type
        get() = value.type
}

data class FnParam(val name: String, val type: Type, val location: Location?)
data class Fn(val fnScope: CompilationScope, val parameters: List<FnParam>, val returnType: Type, val body: Expression, override val location: Location?): Expression {
    override val type: Type = FnType(parameters.map { it.type }, returnType)
    val fnType = type as FnType
}
data class Block(val body: List<Expression>, override val location: Location?): Expression {
    override val type: Type = body.lastOrNull()?.type ?: Type.unit
}

data class FnCall(val enclosingScope: CompilationScope, val function: Expression, val parameters: List<Expression>, override val location: Location?): Expression {
    override val type: Type
        get() {
            return when (val fnType = function.type) {
                is FnType -> fnType.returnType
                is OverloadedFnType -> fnType.getType(parameters.map { it.type })?.returnType ?: Type.undefined
                else -> Type.undefined
            }
        }
}

data class IfElse(val condition: Expression, val thenBranch: Expression, val elseBranch: Expression?, override val location: Location?) : Expression {
    // FIXME: this should choose broader type
    override val type: Type = if (elseBranch != null) thenBranch.type else Type.unit
}

data class InfixOp(val op: String, val left: Expression, val right: Expression, override val location: Location?) : Expression {
    // FIXME: this should probably choose broader type
    override val type: Type = left.type
}

data class PrefixOp(val op: String, val expr: Expression, override val location: Location?) : Expression {
    override val type: Type = expr.type
}

data class Cast(val expression: Expression, val targetType: Type, override val location: Location?) : Expression {
    override val type: Type = targetType
}

data class CompilationScope(private val symbols: MutableMap<String, Type> = mutableMapOf(),
                            private val parent: CompilationScope? = null) {

    fun addSymbol(name: String, type: Type) {
        val existingType = getSymbol(name)
        if (type is FnType) {
            when (existingType) {
                is FnType -> {
                    symbols[name] = OverloadedFnType(setOf(existingType, type))
                }
                is OverloadedFnType -> {
                    symbols[name] = existingType.addFnType(type)
                }
                else -> {
                    symbols[name] = type
                }
            }
        } else {
            symbols[name] = type
        }
    }

    fun getSymbol(name: String): Type? = symbols[name] ?: parent?.getSymbol(name)

    fun containsSymbol(name: String): Boolean = getSymbol(name) != null
}
