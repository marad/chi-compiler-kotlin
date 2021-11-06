package gh.marad.chi

import gh.marad.chi.core.Expression
import gh.marad.chi.core.NewScope
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize

fun asts(code: String, scope: NewScope = NewScope()): List<Expression> = parse(tokenize(code), scope)
fun ast(code: String, scope: NewScope = NewScope()): Expression = asts(code, scope).last()

