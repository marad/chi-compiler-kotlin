package gh.marad.chi

import gh.marad.chi.core.Expression
import gh.marad.chi.core.CompilationScope
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize

fun asts(code: String, scope: CompilationScope = CompilationScope()): List<Expression> = parse(tokenize(code), scope)
fun ast(code: String, scope: CompilationScope = CompilationScope()): Expression = asts(code, scope).last()

