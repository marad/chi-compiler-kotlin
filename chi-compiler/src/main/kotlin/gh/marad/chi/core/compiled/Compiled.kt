package gh.marad.chi.core.compiled

import gh.marad.chi.core.*
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.parser.ChiSource

interface Compiled {
    val sourceSection: ChiSource.Section?
}

data class CompiledBlock(val body: List<Compiled>, override val sourceSection: ChiSource.Section?) : Compiled

data class CallFunction(
    override val sourceSection: ChiSource.Section?
) : Compiled

data class Function(
    val body: CompiledBlock,
    val arguments: List<FnParam>,
    val returnType: Type,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class LongValue(val value: Long, override val sourceSection: ChiSource.Section?) : Compiled
data class StringValue(val value: String, override val sourceSection: ChiSource.Section?) : Compiled
data class FloatValue(val value: Float, override val sourceSection: ChiSource.Section?) : Compiled
data class BoolValue(val value: Boolean, override val sourceSection: ChiSource.Section?) : Compiled
data class UnitValue(override val sourceSection: ChiSource.Section?) : Compiled


data class InterpolatedString(val parts: List<Compiled>, override val sourceSection: ChiSource.Section?) : Compiled
data class ReadPackageVariable(
    val moduleName: String,
    val packageName: String,
    val name: String,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class ReadLocalVariable(val name: String, override val sourceSection: ChiSource.Section?) : Compiled
data class ReadOuterScopeVariable(val name: String, override val sourceSection: ChiSource.Section?) : Compiled
data class ReadFunctionArgument(val index: Int, override val sourceSection: ChiSource.Section?) : Compiled

data class DeclarePackageFunction(
    val mutable: Boolean,
    val public: Boolean,
    val moduleName: String,
    val packageName: String,
    val name: String,
    val arguments: List<Type>,
    val functionBody: Compiled,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class DeclarePackageVariable(
    val moduleName: String,
    val packageName: String,
    val name: String,
    val value: Compiled,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class DeclareLocalVariable(
    val name: String,
    val value: Compiled,
    override val sourceSection: ChiSource.Section?

) : Compiled

data class WritePackageVariable(
    val moduleName: String,
    val packageName: String,
    val name: String,
    val value: Compiled,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class WriteLocalVariable(val name: String, val value: Compiled, override val sourceSection: ChiSource.Section?) :
    Compiled

data class WriteOuterScopeVariable(
    val name: String,
    val value: Compiled,
    override val sourceSection: ChiSource.Section?
) : Compiled

data class WriteFunctionArgument(val index: Int, val value: Compiled, override val sourceSection: ChiSource.Section?) :
    Compiled


fun toCompiled(expr: Expression): Compiled {
    return when (expr) {
        is FnCall -> convertFnCall(expr)
        is Fn -> convertFn(expr)
        is Block -> convertBlock(expr)
        is NameDeclaration -> convertNameDeclaration(expr)
        is Atom -> convertAtom(expr)
        is Program -> convertProgram(expr)
        else -> TODO("Unknown conversion: $expr")
    }
}

fun convertFnCall(expr: FnCall): CallFunction {
    return CallFunction(expr.sourceSection)
}

fun convertFn(expr: Fn): Function {
    return Function(
        body = convertBlock(expr.body),
        arguments = expr.parameters,
        returnType = expr.returnType,
        sourceSection = expr.sourceSection
    )
}

fun convertBlock(expr: Block): CompiledBlock =
    CompiledBlock(
        body = expr.body.map(::toCompiled),
        sourceSection = expr.sourceSection
    )


fun convertNameDeclaration(expr: NameDeclaration): Compiled {
    val scope = expr.enclosingScope
    val symbol = scope.getSymbol(expr.name)
    assert(symbol != null) { "Symbol ${expr.name} nof found!" }

    val scopeType = symbol!!.scopeType
    val valueType = expr.value.type

    return when {
        scopeType == ScopeType.Package && valueType is FnType -> {
            DeclarePackageFunction(
                mutable = expr.mutable,
                public = expr.public,
                moduleName = expr.moduleName,
                packageName = expr.packageName,
                name = expr.name,
                arguments = valueType.paramTypes,
                functionBody = toCompiled(expr.value),
                sourceSection = expr.sourceSection
            )
        }
        scopeType == ScopeType.Package -> {
            DeclarePackageVariable(
                moduleName = expr.moduleName,
                packageName = expr.packageName,
                name = expr.name,
                value = toCompiled(expr.value),
                sourceSection = expr.sourceSection
            )
        }
        else -> {
            DeclareLocalVariable(
                name = expr.name,
                value = toCompiled(expr.value),
                sourceSection = expr.sourceSection
            )
        }
    }
}

fun convertAtom(expr: Atom): Compiled {
    return when (expr.type) {
        Type.intType -> LongValue(expr.value.toLong(), expr.sourceSection)
        Type.string -> StringValue(expr.value, expr.sourceSection)
        Type.floatType -> FloatValue(expr.value.toFloat(), expr.sourceSection)
        Type.bool -> BoolValue(expr.value.toBoolean(), expr.sourceSection)
        Type.unit -> UnitValue(expr.sourceSection)
        else -> TODO("Unhandled atom type: $expr")
    }
}

fun convertProgram(expr: Program): CompiledBlock {
    return CompiledBlock(
        body = expr.expressions.map(::toCompiled),
        sourceSection = expr.sourceSection
    )
}


fun main() {
    val code = """
        val x = 5
        fn hello(): int { 5 }
        hello(10)
    """.trimIndent()
    val result = Compiler.compile(code, GlobalCompilationNamespace())

    val compiled = result.code as CompiledBlock
    compiled.body.forEach { println(it) }
}