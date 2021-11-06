package gh.marad.chi

import gh.marad.chi.core.Expression
import gh.marad.chi.core.parse
import gh.marad.chi.core.tokenize

fun asts(code: String): List<Expression> = parse(tokenize(code))
fun ast(code: String): Expression = asts(code).last()

