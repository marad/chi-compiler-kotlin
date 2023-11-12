package gh.marad.chi.wasm

import gh.marad.chi.core.Compiler
import gh.marad.chi.core.Type
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import kotlin.system.exitProcess

fun main() {
    val code = """
        pub fn add(a: int, b: int): int {
            a + b
        }
    """.trimIndent()
    val ns = GlobalCompilationNamespace()

    ns.getOrCreatePackage("std", "io")
        .scope.addSymbol(
            "println", Type.fn(returnType = Type.unit, Type.intType),
            SymbolType.Local, public = true, mutable = false
        )

    val result = Compiler.compile(code, ns)

    if (result.validate(code)) {
        exitProcess(1)
    }

    val wasmCompiler = WasmModuleCompiler()
    val module = wasmCompiler.compile(result.program, ns)

    module.validate()
    module.print()
}