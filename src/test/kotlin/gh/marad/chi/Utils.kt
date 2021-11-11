package gh.marad.chi

import gh.marad.chi.core.*

fun asts(code: String, scope: CompilationScope = CompilationScope()): List<Expression> = parseProgram(code, scope).expressions
fun ast(code: String, scope: CompilationScope = CompilationScope()): Expression = asts(code, scope).last()

