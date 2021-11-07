package gh.marad.chi.core

import java.util.*


sealed interface Expression {
    val location: Location?
}

data class Atom(val value: String, val type: Type, override val location: Location?): Expression {
    companion object {
        fun unit(location: Location?) = Atom("()", Type.unit, location)
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


data class CompilationScope(private val definedNames: MutableMap<String, Expression> = mutableMapOf(),
                            private val parent: CompilationScope? = null) {
    private val externalNames: MutableMap<String, Type> = mutableMapOf()
    private val parameterDefinitions: MutableMap<String, Type> = mutableMapOf()


    fun addLocalName(name: String, value: Expression) {
        definedNames[name] = value
    }

    fun getLocalName(name: String): Expression? =
        definedNames[name]
            ?: parent?.getLocalName(name)

    fun addParameter(name: String, value: Type) {
        parameterDefinitions[name] = value
    }

    fun getParameter(name: String): Type? = parameterDefinitions[name]

    fun defineExternalName(name: String, type: Type) {
        externalNames[name] = type
    }

    fun getExternalNameType(name: String): Type? = externalNames[name] ?: parent?.getExternalNameType(name)

    override fun toString(): String {
        return "Scope(definedNames=${definedNames.keys}, parent=$parent, externalNames=$externalNames)"
    }

    override fun hashCode(): Int {
        return Objects.hash(definedNames.keys, parent, externalNames)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is CompilationScope) {
            Objects.equals(this.definedNames.keys, other.definedNames.keys)
                    && Objects.equals(this.parent, other.parent)
                    && Objects.equals(this.externalNames, other.externalNames)
        } else {
            false
        }
    }
}
