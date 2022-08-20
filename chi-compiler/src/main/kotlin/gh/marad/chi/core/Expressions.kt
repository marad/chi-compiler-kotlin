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
    override val type: Type
        get() = expressions.lastOrNull()?.type ?: Type.unit
}

data class Package(val moduleName: String, val packageName: String, override val location: Location?) : Expression {
    override val type: Type = Type.unit
}

data class ImportEntry(val name: String, val alias: String?)
data class Import(
    val moduleName: String,
    val packageName: String,
    val packageAlias: String?,
    val entries: List<ImportEntry>,
    override val location: Location?
) : Expression {
    override val type: Type = Type.unit
}

data class DefineVariantType(
    val moduleName: String,
    val packageName: String,
    val name: String,
    val constructors: List<VariantTypeConstructor>,
    val isGeneric: Boolean,
    override val location: Location?
) : Expression {
    override val type: Type = Type.unit
}

data class VariantTypeConstructor(val name: String, val fields: List<VariantTypeField>, val location: Location?)
data class VariantTypeField(val name: String, val type: Type, val location: Location?)

data class Atom(val value: String, override val type: Type, override val location: Location?) : Expression {
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

data class VariableAccess(
    val moduleName: String, val packageName: String, val definitionScope: CompilationScope,
    val name: String, override val location: Location?
) : Expression {
    override val type: Type
        get() = definitionScope.getSymbolType(name) ?: Type.undefined
}

data class FieldAccess(
    val receiver: Expression, val fieldName: String, override val location: Location?, val memberLocation: Location
) : Expression {
    override val type: Type
        get() = (receiver.type as CompositeType).memberType(fieldName) ?: Type.undefined
}

data class FieldAssignment(
    val receiver: Expression, val fieldName: String, val value: Expression, override val location: Location?
) : Expression {
    override val type: Type
        get() = (receiver.type as CompositeType).memberType(fieldName) ?: Type.undefined
}

data class Assignment(
    val definitionScope: CompilationScope, val name: String, val value: Expression,
    override val location: Location?
) : Expression {
    override val type: Type = value.type
}

data class NameDeclaration(
    val enclosingScope: CompilationScope,
    val name: String,
    val value: Expression,
    val mutable: Boolean,
    val expectedType: Type?,
    override val location: Location?
) : Expression {
    override val type: Type = expectedType ?: value.type
}

data class Group(val value: Expression, override val location: Location?) : Expression {
    override val type: Type
        get() = value.type
}

data class FnParam(val name: String, val type: Type, val location: Location?)
data class Fn(
    val fnScope: CompilationScope,
    val genericTypeParameters: List<GenericTypeParameter>,
    val parameters: List<FnParam>,
    val returnType: Type,
    val body: Block,
    override val location: Location?
) : Expression {
    override val type: Type = FnType(genericTypeParameters, parameters.map { it.type }, returnType)
}

data class Block(val body: List<Expression>, override val location: Location?) : Expression {
    override val type: Type = body.lastOrNull()?.type ?: Type.unit
}

data class FnCall(
    val enclosingScope: CompilationScope,
    val name: String,
    val function: Expression,
    val callTypeParameters: List<Type>,
    val parameters: List<Expression>,
    override val location: Location?
) : Expression {
    override val type: Type
        get() {
            val functionType: FnType = when (val fnType = function.type) {
                is FnType -> fnType
                is OverloadedFnType -> fnType.getType(parameters.map { it.type }) ?: return Type.undefined
                else -> return Type.undefined
            }
            return resolveGenericType(
                functionType,
                callTypeParameters,
                parameters,
            )
        }
}

data class IfElse(
    val condition: Expression,
    val thenBranch: Expression,
    val elseBranch: Expression?,
    override val location: Location?
) : Expression {
    // FIXME: this should choose broader type
    override val type: Type = if (elseBranch != null) thenBranch.type else Type.unit
}

data class InfixOp(val op: String, val left: Expression, val right: Expression, override val location: Location?) :
    Expression {
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

data class IndexOperator(
    val variable: Expression,
    val index: Expression,
    override val location: Location?
) : Expression {
    override val type: Type
        get() {
            assert(variable.type.isIndexable()) { "Cannot index types other than array!" }
            return variable.type.indexedElementType()
        }
}

data class IndexedAssignment(
    val variable: Expression,
    val index: Expression,
    val value: Expression,
    override val location: Location?
) : Expression {
    override val type: Type
        get() {
            assert(variable.type.isIndexable()) { "Cannot index types other than array!" }
            return variable.type.indexedElementType()
        }
}
