package gh.marad.chi.core

data class LocationPoint(val line: Int, val column: Int)

data class Location(val start: LocationPoint, val end: LocationPoint, val startIndex: Int, val endIndex: Int) {
    val formattedPosition = "${start.line}:${start.column}"
}


sealed interface Expression {
    val location: Location?
    val type: Type
}

data class Program(val expressions: List<Expression>) : Expression {
    override val location: Location? = null
    override val type: Type = expressions.lastOrNull()?.type ?: Type.unit
}

data class Package(val moduleName: String, val packageName: String, override val location: Location?) : Expression {
    override val type: Type = Type.unit
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
        get() = enclosingScope.getSymbolType(name) ?: Type.undefined
}

data class Assignment(val enclosingScope: CompilationScope, val name: String, val value: Expression,
                      override val location: Location?) : Expression {
    override val type: Type = value.type
}

data class NameDeclaration(val enclosingScope: CompilationScope, val name: String, val value: Expression, val immutable: Boolean, val expectedType: Type?, override val location: Location?): Expression {
    override val type: Type = expectedType ?: value.type
}

data class Group(val value: Expression, override val location: Location?): Expression {
    override val type: Type
        get() = value.type
}

data class FnParam(val name: String, val type: Type, val location: Location?)
data class Fn(val fnScope: CompilationScope, val parameters: List<FnParam>, val returnType: Type, val body: Block, override val location: Location?): Expression {
    override val type: Type = FnType(parameters.map { it.type }, returnType)
    val fnType = type as FnType
}
data class Block(val body: List<Expression>, override val location: Location?): Expression {
    override val type: Type = body.lastOrNull()?.type ?: Type.unit
}

data class FnCall(val enclosingScope: CompilationScope, val name: String, val function: Expression, val parameters: List<Expression>, override val location: Location?): Expression {
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
    override val type: Type = when (op) {
        in listOf("==", "!=", "<", ">", "<=", ">=", "&&", "||") -> Type.bool
        else -> left.type
    }
}

data class PrefixOp(val op: String, val expr: Expression, override val location: Location?) : Expression {
    override val type: Type = expr.type
}

data class Cast(val expression: Expression, val targetType: Type, override val location: Location?) : Expression {
    override val type: Type = targetType
}

data class WhileLoop(val condition: Expression, val loop: Expression, override val location: Location?) : Expression {
    override val type: Type = Type.unit
}

enum class SymbolScope { Local, Argument }
data class SymbolInfo(val name: String, val type: Type, val scope: SymbolScope, val slot: Int)
data class CompilationScope(private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()

    fun addSymbol(name: String, type: Type, scope: SymbolScope) {
        val existingType = getSymbolType(name)
        val finalType = if (type is FnType) {
            when (existingType) {
                is FnType ->
                    OverloadedFnType(setOf(existingType, type))
                is OverloadedFnType ->
                    existingType.addFnType(type)
                else ->
                    type
            }
        } else {
            type
        }
        symbols[name] = SymbolInfo(name, finalType, scope, -1)
    }

    fun getSymbolType(name: String): Type? = symbols[name]?.type ?: parent?.getSymbolType(name)

    fun getSymbol(name: String): SymbolInfo? = symbols[name] ?: parent?.getSymbol(name)

    fun containsSymbol(name: String): Boolean = getSymbolType(name) != null

    fun isLocalSymbol(name: String) = symbols[name] != null

    fun updateSlot(name: String, slot: Int) {
        symbols.compute(name) { _, symbol ->
            symbol?.copy(slot = slot)
        }
    }

    fun getLocals(): List<SymbolInfo> = symbols.values.filter { it.scope == SymbolScope.Local }
    fun getArguments(): List<SymbolInfo> = symbols.values.filter { it.scope == SymbolScope.Argument }

    private var nextLocalSlot = 0
    private var nextArgumentSlot = 0
    private fun nextSlot(scope: SymbolScope) =
        if (scope == SymbolScope.Local) {
            nextLocalSlot++
        } else {
            nextArgumentSlot++
        }
}
