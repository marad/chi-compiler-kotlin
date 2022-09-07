package gh.marad.chi.core

import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.parser.ChiSource

sealed interface Expression {
    val sourceSection: ChiSource.Section?
    val type: Type
}

data class Program(val expressions: List<Expression>, override val sourceSection: ChiSource.Section? = null) :
    Expression {
    override val type: Type
        get() = expressions.lastOrNull()?.type ?: Type.unit
}

data class Package(val moduleName: String, val packageName: String, override val sourceSection: ChiSource.Section?) :
    Expression {
    override val type: Type = Type.unit
}

data class ImportEntry(val name: String, val alias: String?)
data class Import(
    val moduleName: String,
    val packageName: String,
    val packageAlias: String?,
    val entries: List<ImportEntry>,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type = Type.unit
}

data class DefineVariantType(
    val baseVariantType: VariantType,
    val constructors: List<VariantTypeConstructor>,
    override val sourceSection: ChiSource.Section?,
) : Expression {
    override val type: Type = Type.unit
    val moduleName get() = baseVariantType.moduleName
    val packageName get() = baseVariantType.packageName
    val name get() = baseVariantType.simpleName
}

data class VariantTypeConstructor(
    val name: String,
    val fields: List<VariantTypeField>,
    val sourceSection: ChiSource.Section?
) {
    fun toVariant() = VariantType.Variant(name, fields.map { it.toVariantField() })
}

data class VariantTypeField(val name: String, val type: Type, val sourceSection: ChiSource.Section?) {
    fun toVariantField() = VariantType.VariantField(name, type)
}

data class Atom(val value: String, override val type: Type, override val sourceSection: ChiSource.Section?) :
    Expression {
    companion object {
        fun unit(sourceSection: ChiSource.Section? = null) = Atom("()", Type.unit, sourceSection)
        fun int(value: Long, sourceSection: ChiSource.Section?) = Atom("$value", Type.intType, sourceSection)
        fun float(value: Float, sourceSection: ChiSource.Section?) = Atom("$value", Type.floatType, sourceSection)
        fun bool(b: Boolean, sourceSection: ChiSource.Section?) = if (b) t(sourceSection) else f(sourceSection)
        fun t(sourceSection: ChiSource.Section?) = Atom("true", Type.bool, sourceSection)
        fun f(sourceSection: ChiSource.Section?) = Atom("false", Type.bool, sourceSection)
        fun string(value: String, sourceSection: ChiSource.Section?) = Atom(value, Type.string, sourceSection)
    }
}

data class VariableAccess(
    val moduleName: String, val packageName: String, val definitionScope: CompilationScope,
    val name: String, override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type
        get() = definitionScope.getSymbolType(name) ?: Type.undefined
}

data class FieldAccess(
    val receiver: Expression,
    val fieldName: String,
    override val sourceSection: ChiSource.Section?,
    val memberSection: ChiSource.Section?,
) : Expression {
    override val type: Type
        get() = (receiver.type as CompositeType).memberType(fieldName) ?: Type.undefined
}

data class FieldAssignment(
    val receiver: Expression,
    val fieldName: String,
    val value: Expression,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type
        get() = (receiver.type as CompositeType).memberType(fieldName) ?: Type.undefined
}

data class Assignment(
    val definitionScope: CompilationScope, val name: String, val value: Expression,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type = value.type
}

data class NameDeclaration(
    val enclosingScope: CompilationScope,
    val name: String,
    val value: Expression,
    val mutable: Boolean,
    val expectedType: Type?,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type = expectedType ?: value.type
}

data class Group(val value: Expression, override val sourceSection: ChiSource.Section?) : Expression {
    override val type: Type
        get() = value.type
}

data class FnParam(val name: String, val type: Type, val sourceSection: ChiSource.Section?)
data class Fn(
    val fnScope: CompilationScope,
    val genericTypeParameters: List<GenericTypeParameter>,
    val parameters: List<FnParam>,
    val returnType: Type,
    val body: Block,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type = FnType(genericTypeParameters, parameters.map { it.type }, returnType)
}

data class Block(val body: List<Expression>, override val sourceSection: ChiSource.Section?) : Expression {
    override val type: Type = body.lastOrNull()?.type ?: Type.unit
}

data class FnCall(
    val name: String,
    val function: Expression,
    val callTypeParameters: List<Type>,
    val parameters: List<Expression>,
    override val sourceSection: ChiSource.Section?
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
    override val sourceSection: ChiSource.Section?
) : Expression {
    // FIXME: this should choose broader type
    override val type: Type = if (elseBranch != null) thenBranch.type else Type.unit
}

data class InfixOp(
    val op: String,
    val left: Expression,
    val right: Expression,
    override val sourceSection: ChiSource.Section?
) :
    Expression {
    // FIXME: this should probably choose broader type
    override val type: Type = when (op) {
        in listOf("==", "!=", "<", ">", "<=", ">=", "&&", "||") -> Type.bool
        else -> left.type
    }
}

data class PrefixOp(val op: String, val expr: Expression, override val sourceSection: ChiSource.Section?) : Expression {
    override val type: Type = expr.type
}

data class Cast(val expression: Expression, val targetType: Type, override val sourceSection: ChiSource.Section?) :
    Expression {
    override val type: Type = targetType
}

data class WhileLoop(val condition: Expression, val loop: Expression, override val sourceSection: ChiSource.Section?) :
    Expression {
    override val type: Type = Type.unit
}

data class IndexOperator(
    val variable: Expression,
    val index: Expression,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type
        get() {
//            assert(variable.type.isIndexable()) { "Cannot index types other than array!" }
            return variable.type.indexedElementType()
        }
}

data class IndexedAssignment(
    val variable: Expression,
    val index: Expression,
    val value: Expression,
    override val sourceSection: ChiSource.Section?
) : Expression {
    override val type: Type
        get() {
//            assert(variable.type.isIndexable()) { "Cannot index types other than array!" }
            return variable.type.indexedElementType()
        }
}

data class Is(val value: Expression, val variantName: String, override val sourceSection: ChiSource.Section?) :
    Expression {
    override val type: Type = Type.bool
}
