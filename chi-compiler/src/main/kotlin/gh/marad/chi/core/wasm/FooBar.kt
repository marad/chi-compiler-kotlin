package gh.marad.chi.core.wasm

import gh.marad.chi.core.*
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.github.kawamuray.wasmtime.*
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder
import java.nio.file.Files
import java.nio.file.Path

data class IntermediateModule(
    val pkg: Package,
    val imports: List<Import>,
    val globals: List<NameDeclaration>,
    val functions: List<NameDeclaration>,
    val topLevelCode: List<Expression>,
) {
    fun generateWat(ns: GlobalCompilationNamespace): String {
        val sb = StringBuilder()
        val moduleName = "${pkg.moduleName}/${pkg.packageName}"
        sb.appendLine("(module $${moduleName}")
        // define imports
        imports.forEach { sb.appendImport(it, ns) }

        // declare globals
        globals.forEach(sb::appendGlobalDefinition)

        // declare functions
        val ctx = EmitContext(mutableListOf())
        sb.appendLine(prelude)
        functions.forEach { sb.appendFunctionDeclaration(ctx, it.public, it.name, it.value as Fn) }

        // lambda functions can also define lambda functions :o
        ctx.lambdaFunctions.forEach { sb.appendFunctionDeclaration(ctx, public = false, it.name, it.fn) }

        // generate init function
        generateStart(sb)

        sb.append(')') // end module
        return sb.toString()
    }

    private fun generateStart(sb: StringBuilder) {
        sb.append("(func \$__init_module__")
        sb.appendLine()
        sb.appendFunctionBody(EmitContext(mutableListOf()), topLevelCode)
        sb.appendLine(')')
        sb.appendLine("(start \$__init_module__)")
    }

    private val prelude = """
    """.trimIndent()
}

data class EmitContext(
    val lambdaFunctions: MutableList<LambdaFunction>
) {
    fun nextLambdaFunctionName(): String = "lambda${lambdaFunctions.size}"
}

data class LambdaFunction(val name: String, val fn: Fn)


fun StringBuilder.appendImport(import: Import, ns: GlobalCompilationNamespace) {
    val moduleName = "${import.moduleName}/${import.packageName}"
    import.entries.forEach {
        if (!it.isTypeImport) {
            val importedName = it.alias ?: it.name

            val pkg = ns.getOrCreatePackage(import.moduleName, import.packageName)
            val symbol = pkg.scope.getSymbol(it.name)!!
            val type = symbol.type

            when (type) {
                is FnType -> {
                    val paramTypes = type.paramTypes.joinToString(" ", transform = ::mapType)
                    val resultType = mapType(type.returnType)
                    appendLine("(import \"$moduleName\" \"${it.name}\" (func $$importedName (param $paramTypes) (result $resultType)))")
                }

                else -> throw RuntimeException("Unsupported import for type: $type")
            }

        }
    }
}

fun StringBuilder.appendFunctionBody(ctx: EmitContext, body: List<Expression>) {
    // find required if result locals
    val locals = mutableSetOf<String>()

    body.forEach {
        forEachAst(it) { exp ->
            when (exp) {
                is IfElse ->
                    locals.add(
                        "(local ${exp.resultVarName()} ${mapType(exp.type)})"
                    )

                is NameDeclaration -> {
                    val symbol = exp.enclosingScope.getSymbol(exp.name)
                    if (symbol == null || symbol.scopeType != ScopeType.Package) {
                        locals.add(
                            "(local $${exp.name} ${mapType(exp.type)})"
                        )
                    }
                }
            }

        }
    }

    locals.forEach(this::appendLine)

    body.forEach { this.appendWat(ctx, it) }
}

fun Program.toIntermediateModule(): IntermediateModule {
    var exps = expressions
    val pkg = if (expressions.first() is Package) {
        exps = exps.drop(1)
        expressions.first()
    } else {
        Package(CompilationDefaults.defaultModule, CompilationDefaults.defaultPacakge, null)
    }

    val imports = exps.filterIsInstance<Import>()
    exps = exps.dropWhile { it is Import }

    val nameDeclarations = exps.filterIsInstance(NameDeclaration::class.java)
    val functions = nameDeclarations.filter { it.value is Fn }
    return IntermediateModule(
        pkg = pkg as Package,
        imports = imports,
        globals = nameDeclarations.filter { it.value !is Fn },
        functions = functions,
        topLevelCode = exps.filter { it !in functions }
    )
}


fun mapType(type: Type): String = when (type) {
    Type.intType -> "i64"
    Type.floatType -> "f32"
    Type.bool -> "i32"
    Type.unit -> ""
    is FnType -> "funcref"
    else -> throw RuntimeException("Unmapped type: $type")
}


fun Type.defaultWasmValue(): String = when (this) {
    Type.intType -> "0"
    Type.bool -> "0"
    else -> throw RuntimeException("Unmapped type: $this")
}

fun StringBuilder.appendConst(type: Type, value: String) {
    when (type) {
        Type.intType -> appendLine("(i64.const $value)")
        Type.floatType -> appendLine("(f32.const $value)")
        Type.bool -> appendLine("(i32.const $value)")
        else -> throw RuntimeException("Cannot create const value '$value' for type: '$type'")
    }
}

fun StringBuilder.appendGlobalDefinition(global: NameDeclaration) {
    // define global
    append("(global $${global.name}")
    // export it if public
    if (global.public) {
        append(" (export \"${global.name}\")")
    }

    // FIXME: global is mutable so it's initialized with 0
    //        it's made like that so we can later initialize it in start section
    append(" (mut ${mapType(global.type)}) ")

    // add default value
    appendConst(global.type, global.type.defaultWasmValue())
    appendLine(')')
}

fun StringBuilder.appendFunctionDeclaration(ctx: EmitContext, public: Boolean, name: String, fn: Fn) {
    append("(func \$${name}")
    if (public) {
        append(" (export \"${name}\")")
    }

    fn.parameters.forEach { param ->
        append(" (param \$${param.name} ${mapType(param.type)})")
    }

    if (fn.returnType != Type.unit) {
        append(" (result ${mapType(fn.returnType)})")
    }

    appendLine()

    appendFunctionBody(ctx, fn.body.body)

    if (fn.returnType == Type.unit && fn.body.body.isNotEmpty() && fn.body.body.lastOrNull()?.type != Type.unit) {
        appendLine("drop")
    }

    appendLine(')')
}

fun StringBuilder.appendWat(ctx: EmitContext, exp: Expression) {
    when (exp) {
        is Atom -> {
            when (exp.type) {
                Type.intType -> appendConst(exp.type, exp.value)
                Type.floatType -> appendConst(exp.type, exp.value)
                Type.bool -> if (exp.value == "true") {
                    appendConst(exp.type, "1")
                } else {
                    appendConst(exp.type, "0")
                }
            }

        }

        is NameDeclaration -> {
            val symbol = exp.enclosingScope.getSymbol(exp.name) ?: throw RuntimeException("Missing symbol ${exp.name}")
            val isGlobal = symbol.scopeType == ScopeType.Package
            appendWat(ctx, exp.value)
            if (isGlobal) {
                appendLine("(global.set $${exp.name})")
            } else {
                appendLine("(local.set $${exp.name})")
            }
        }

        is VariableAccess -> {
            // FIXME: this only supports local variables (no imports yet)
            val symbol = exp.definitionScope.getSymbol(exp.name) ?: throw RuntimeException("Missing symbol ${exp.name}")
            val isGlobal = symbol.scopeType == ScopeType.Package
            if (isGlobal) {
                appendLine("(global.get $${exp.name})")
            } else {
                appendLine("(local.get $${exp.name})")
            }
        }

        is Assignment -> {
            appendWat(ctx, exp.value)
            val symbol = exp.definitionScope.getSymbol(exp.name) ?: throw RuntimeException("Missing symbol ${exp.name}")
            val isGlobal = symbol.scopeType == ScopeType.Package
            if (isGlobal) {
                appendLine("(global.set $${exp.name})")
                appendLine("(global.get $${exp.name})")
            } else {
                appendLine("(local.tee $${exp.name})")
            }
        }

        is FnCall -> {

            val fnType = exp.function.type as FnType
            exp.parameters.forEach { param ->
                appendWat(ctx, param)
            }
            if (exp.function is VariableAccess && exp.function.definitionScope.type == ScopeType.Package) {
                // this is a static function call
                appendLine("call $${exp.function.name}")
            } else {
                appendWat(ctx, exp.function)
                // add indirect call
                append("(call_indirect ")
                append("(param ")
                append(fnType.paramTypes.joinToString(" ") { mapType(it) })
                appendLine(')')
            }

        }

        is InfixOp -> {
            appendWat(ctx, exp.left)
            appendWat(ctx, exp.right)
            val type = exp.left.type
            val opType = mapType(type)
            when (exp.op) {
                "+" -> appendLine("$opType.add")
                "-" -> appendLine("$opType.sub")
                "*" -> appendLine("$opType.mul")
                "/" -> appendLine("$opType.div_s")
                "%" -> appendLine("$opType.rem_s")
                "&&" -> appendLine("$opType.and")
                "&" -> appendLine("$opType.and")
                "||" -> appendLine("$opType.or")
                "|" -> appendLine("$opType.or")
                "<<" -> appendLine("$opType.shl")
                ">>" -> appendLine("$opType.shr")
                "==" -> appendLine("$opType.eq")
                "!=" -> appendLine("$opType.ne")
                "<" -> if (type.isInteger()) appendLine("$opType.lt_s") else appendLine("$opType.lt")
                "<=" -> if (type.isInteger()) appendLine("$opType.le_s") else appendLine("$opType.le")
                ">" -> if (type.isInteger()) appendLine("$opType.gt_s") else appendLine("$opType.gt")
                ">=" -> if (type.isInteger()) appendLine("$opType.ge_s") else appendLine("$opType.ge")
                else -> throw RuntimeException("Unhandled binary operation: ${exp.op}")
            }
        }

        is IfElse -> {
            val hasValue = exp.type != Type.unit
            val ifValue = exp.resultVarName()
            appendWat(ctx, exp.condition)
            appendLine("(if")
            appendLine("(then")
            appendWat(ctx, exp.thenBranch)
            if (!hasValue) appendLine("drop")
            else appendLine("local.set $ifValue")
            appendLine(")")
            if (exp.elseBranch != null) {
                appendLine("(else")
                appendWat(ctx, exp.elseBranch)
                if (!hasValue) appendLine("drop")
                else appendLine("local.set $ifValue")
                appendLine(")")
            }
            appendLine(")")
            if (hasValue) appendLine("local.get $ifValue")
        }

        is Block -> {
            exp.body.forEach { appendWat(ctx, it) }
        }

        is Group -> appendWat(ctx, exp.value)

        // proste
        is PrefixOp -> {
            when (exp.op) {
                "!" -> {
                    val t = mapType(exp.type)
                    appendWat(ctx, exp.expr)
                    appendLine("$t.eqz")
                }

                else -> TODO()
            }
        }

        is Cast -> {
            appendWat(ctx, exp.expression)
            val fromType = mapType(exp.expression.type)
            val toType = mapType(exp.targetType)
            when (fromType to toType) {
                "i32" to "i64" -> appendLine("i64.extend_i32_s")
                "i64" to "i32" -> appendLine("i32.wrap_i64")
                "i32" to "i32" -> {}
                "i64" to "i64" -> {}
                else -> TODO("Can't convert from $fromType to $toType")
            }
        }

        // ten wymaga posiadania struktur
        is Is -> {
            // proste sprawdzanie typu
            if (exp.typeOrVariant == exp.value.type.name) {
                appendConst(Type.bool, "1")
            } else {
                appendConst(Type.bool, "0")
            }

            // TODO: implementacja sprawdzenia variantu wymaga najpierw implementacji struktur
            //       struktury powinny mieć informację o tym jakim są wariantem w runtime
        }

        // funkcje
        is Fn -> {
            // FIXME: actually register the function
            val name = ctx.nextLambdaFunctionName()
            val lambda = LambdaFunction(name, exp)
            ctx.lambdaFunctions.add(lambda)
            appendLine("(ref.func $$name)")
        }

        // while
        is Break -> TODO()
        is Continue -> TODO()
        is WhileLoop -> TODO()

        // struktury
        is DefineVariantType -> TODO()
        is FieldAccess -> TODO()
        is FieldAssignment -> TODO()

        // efekty (wymaga wyjątków lub kontynuacji)
        is EffectDefinition -> TODO()
        is Handle -> TODO()

        // tablice
        is IndexOperator -> TODO()
        is IndexedAssignment -> TODO()

        // stringi
        is InterpolatedString -> TODO()

        // Tych nie powinno być tutaj - muszą zostać obsłużone wcześniej
        is Import -> TODO()
        is Package -> TODO()
        is Program -> TODO()
    }
}

fun IfElse.resultVarName(): String = "\$__${mapType(type)}_ifResult__"

fun main() {
    val ns = GlobalCompilationNamespace().apply {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol(
            "println",
            FnType(emptyList(), listOf(Type.intType), Type.unit),
            SymbolType.Local,
            public = true,
            mutable = false
        )
        setPackageScope("std", "io", scope)
    }

    val code = Compiler.compile(
        """
            package compilation/test
            
            fn foo(): () -> int {
                val x = { 10 }
            }
        """.trimIndent(),
        ns
    )

    val module = code.program.toIntermediateModule()
    val wat = module.generateWat(ns)
    println(wat)
    val path = Path.of("test.wat")
    Files.writeString(path, wat)


//    val wasi = WasiCtx(0)
//    wasi.pushArg()
    val wasi = WasiCtxBuilder()
        .args(listOf("--experimental-wasm-typed_funcref"))
        .build()
    val store = Store.withoutData(wasi)
    val mod = Module.fromFile(store.engine(), "test.wat")
    val instance = Instance(
        store, mod, listOf(
            Extern.fromFunc(Func(store, FuncType.emptyResults(Val.Type.I64)) { caller, params, results ->
                println(params[0].i64())
            })
        )
    )
}