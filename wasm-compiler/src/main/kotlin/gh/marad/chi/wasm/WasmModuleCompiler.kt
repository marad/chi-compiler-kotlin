package gh.marad.chi.wasm

import gh.marad.binaryen.*
import gh.marad.binaryen.Function
import gh.marad.binaryen.Type as wt
import gh.marad.binaryen.Expression as WasmExpr
import gh.marad.chi.core.*
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.wasm.toIntermediateModule
import jdk.incubator.vector.VectorOperators.Binary

class WasmModuleCompiler {
    private val m = WasmModule.create()
    private val startExprs = listOf<Expression>()
    fun compile(program: Program, ns: GlobalCompilationNamespace): WasmModule {
        val intModule = program.toIntermediateModule()

        // define imports
        intModule.imports.forEach {
            compileImport(it, ns)
        }

        // declare globals with default values for now
        intModule.globals.forEach {
            m.addGlobal(it.name, mapType(it.type), mutable_ = it.mutable, m.const(it.type.deafultWasmValue()))
        }

        intModule.functions.forEach {
            compileFunction(it)
        }

        return m
    }


    private fun compileImport(import: Import, ns: GlobalCompilationNamespace) {
        val wasmModuleName = "${import.packageName}/${import.packageName}"
        import.entries.forEach {
            val importedName = it.alias ?: it.name

            val pkg = ns.getOrCreatePackage(import.moduleName, import.packageName)
            val symbol = pkg.scope.getSymbol(it.name)!!
            val type = symbol.type

            when (type) {
                is FnType -> {
                    val paramTypes = wt.create(*type.paramTypes.map(::mapType).toTypedArray())
                    val resultType = mapType(type.returnType)
                    Binaryen.getInstance().BinaryenAddFunctionImport(
                        m, importedName, wasmModuleName, it.name, paramTypes, resultType
                    )
                }

                else -> throw RuntimeException("Unsupported import for type: $type")
            }
        }
    }

    fun compileFunction(funcDef: NameDeclaration): Function {
        val fn = funcDef.value as Fn
        val compiledBody = compileBlock(fn.body, fn.parameters)
        val type = fn.type as FnType
        val paramTypes = wt.create(*type.paramTypes.map(::mapType).toTypedArray())
        val resultType = mapType(type.returnType)
        val localVarTypes = compiledBody.localVariables.map { mapType(it.type) }
        m.exportFunction(funcDef.name)
        return m.addFunction(funcDef.name, paramTypes, resultType, localVarTypes, compiledBody.expr)
    }

    fun compileBlock(block: Block, params: List<FnParam> = emptyList()): CompiledBlock {
        val ctx = BlockContext(m, block.type, params)
        block.body.forEach {
            ctx.addExpr(compileExpr(ctx, it))
        }
        return ctx.build()
    }

    fun compileExpr(ctx: BlockContext, exp: Expression): WasmExpr {
        return when (exp) {
            is Atom -> compileAtom(ctx, exp)
            is Assignment -> TODO()
            is Block -> TODO()
            is Break -> TODO()
            is Cast -> TODO()
            is Continue -> TODO()
            is DefineVariantType -> TODO()
            is EffectDefinition -> TODO()
            is FieldAccess -> TODO()
            is FieldAssignment -> TODO()
            is Fn -> TODO()
            is FnCall -> TODO()
            is Group -> TODO()
            is Handle -> TODO()
            is IfElse -> TODO()
            is Import -> TODO()
            is IndexOperator -> TODO()
            is IndexedAssignment -> TODO()
            is InfixOp -> compileInfixOp(ctx, exp)
            is InterpolatedString -> TODO()
            is Is -> TODO()
            is NameDeclaration -> TODO()
            is Package -> TODO()
            is PrefixOp -> {
                println(exp)
                TODO()
            }

            is Program -> TODO()
            is VariableAccess -> compileVariableAccess(ctx, exp)
            is WhileLoop -> TODO()
        }
    }

    fun compileAtom(ctx: BlockContext, exp: Atom): WasmExpr = m.const(
        when (exp.type) {
            Type.intType -> Literal.i64(exp.value.toLong())
            Type.floatType -> Literal.f32(exp.value.toFloat())
            Type.bool -> if (exp.value == "true") Literal.i32(1) else Literal.i32(0)
            else -> TODO()
        }
    )

    fun compileInfixOp(ctx: BlockContext, exp: InfixOp): WasmExpr {
        val left = compileExpr(ctx, exp.left)
        val right = compileExpr(ctx, exp.right)
        return m.binary(mapBinaryOp(exp.op, exp.left.type), left, right)
    }

    fun compileVariableAccess(ctx: BlockContext, exp: VariableAccess): WasmExpr {
        val symbol = exp.definitionScope.getSymbol(exp.name) ?: throw RuntimeException("Missing symbol ${exp.name}")
        val isGlobal = symbol.scopeType == ScopeType.Package
        return if (isGlobal) {
            m.globalGet(exp.name, mapType(exp.type))
        } else {
            val index = ctx.getLocalIndex(exp.name)
            m.localGet(index, mapType(exp.type))
        }
    }

    data class CompiledBlock(
        val expr: WasmExpr,
        val lambdas: List<Pair<String, Fn>>,
        val localVariables: List<NameDeclaration>
    )

    class BlockContext(val m: WasmModule, val type: Type, val params: List<FnParam>) {
        val lambdas: List<Pair<String, Fn>> get() = _lambdas
        val locals: List<NameDeclaration> get() = _localVariables
        private val _lambdas = mutableListOf<Pair<String, Fn>>()
        private val _localVariables = mutableListOf<NameDeclaration>()
        private val _exprs = mutableListOf<WasmExpr>()
        private val indexes = mutableMapOf<String, Index>()
        fun addLambdaAndGetName(fn: Fn): String = TODO()

        init {
            params.forEachIndexed { index, param ->
                indexes[param.name] = index
            }
        }

        fun addLocalVariable(localDef: NameDeclaration) {
            _localVariables.add(localDef)
            indexes[localDef.name] = indexes.size
        }

        fun addExpr(expr: WasmExpr) {
            _exprs.add(expr)
        }

        fun getLocalIndex(name: String): Index {
            return indexes[name] ?: TODO("Variable $name referenced before creation")
        }

        fun build(): CompiledBlock {
            val name = "block" // TODO: generate random name
            val exprs = _exprs.ifEmpty { listOf(m.nop()) }
            val block = m.block(name, exprs, mapType(type))
            return CompiledBlock(block, _lambdas, _localVariables)
        }
    }
}

fun Type.deafultWasmValue(): Literal.ByValue = when (this) {
    Type.intType -> Literal.i64(0)
    Type.floatType -> Literal.f32(0f)
    Type.bool -> Literal.i32(0)
    else -> throw RuntimeException("Unmapped type: $this")
}

fun mapType(type: Type): wt = when (type) {
    Type.intType -> wt.i64
    Type.floatType -> wt.f64
    Type.bool -> wt.i32
    Type.unit -> wt.none
    is FnType -> wt.funcref
    else -> throw RuntimeException("Unmapped type: $type")
}

fun mapBinaryOp(op: String, type: Type): Op {
    return when (op) {
        "+" -> type.add()
        "-" -> type.sub()
        "*" -> type.mul()
        "/" -> type.div()
        "%" -> type.rem()
        "&&" -> type.and()
        "&" -> type.and()
        "||" -> type.or()
        "|" -> type.or()
        "<<" -> type.shl()
        ">>" -> type.shr()
        "==" -> type.eq()
        "!=" -> type.neq()
        "<" -> type.lt()
        "<=" -> type.lte()
        ">" -> type.gt()
        ">=" -> type.gte()
        else -> throw RuntimeException("Unhandled binary operation: ${op}")
    }
}

fun Type.add(): Op = when (this) {
    Type.intType -> Ops.addInt64
    Type.floatType -> Ops.addFloat32
    else -> TODO("Add operation is not supported for type $this")
}

fun Type.sub(): Op = when (this) {
    Type.intType -> Ops.subInt64
    Type.floatType -> Ops.subFloat32
    else -> TODO("Subtract operation is not supported for type $this")
}

fun Type.mul(): Op = when (this) {
    Type.intType -> Ops.mulInt64
    Type.floatType -> Ops.mulFloat32
    else -> TODO("Multiply operation is not supported for type $this")
}

fun Type.div(): Op = when (this) {
    Type.intType -> Ops.divSInt64
    Type.floatType -> Ops.divFloat32
    else -> TODO("Divide operation is not supported for type $this")
}

fun Type.rem(): Op = when (this) {
    Type.intType -> Ops.remSInt64
    else -> TODO("Reminder operation is not supported for type $this")
}

fun Type.and(): Op = when (this) {
    Type.intType -> Ops.andInt64
    Type.bool -> Ops.andInt32
    else -> TODO("And operation is not supported for type $this")
}

fun Type.or(): Op = when (this) {
    Type.intType -> Ops.andInt64
    Type.bool -> Ops.orInt32
    else -> TODO("Or operation is not supported for type $this")
}

fun Type.shl(): Op = when (this) {
    Type.intType -> Ops.shlInt64
    else -> TODO("Shift left operation is not supported for type $this")
}

fun Type.shr(): Op = when (this) {
    Type.intType -> Ops.shrSInt64
    else -> TODO("Shift right operation is not supported for type $this")
}

fun Type.eq(): Op = when (this) {
    Type.intType -> Ops.eqInt64
    Type.bool -> Ops.eqInt32
    Type.floatType -> Ops.eqFloat32
    else -> TODO("Equals operation is not supported for type $this")
}

fun Type.neq(): Op = when (this) {
    Type.intType -> Ops.neInt64
    Type.bool -> Ops.neInt32
    Type.floatType -> Ops.neFloat32
    else -> TODO("Not-equal operation is not supported for type $this")
}

fun Type.lt(): Op = when (this) {
    Type.intType -> Ops.ltSInt64
    Type.floatType -> Ops.ltFloat32
    else -> TODO("Less than operation is not supported for type $this")
}

fun Type.lte(): Op = when (this) {
    Type.intType -> Ops.leSInt64
    Type.floatType -> Ops.leFloat32
    else -> TODO("Less than or equal operation is not supported for type $this")
}

fun Type.gt(): Op = when (this) {
    Type.intType -> Ops.gtSInt64
    Type.floatType -> Ops.gtFloat32
    else -> TODO("Greater than operation is not supported for type $this")
}

fun Type.gte(): Op = when (this) {
    Type.intType -> Ops.geSInt64
    Type.floatType -> Ops.geFloat32
    else -> TODO("Greater than or equal operation is not supported for type $this")
}
