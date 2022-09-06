package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.Expression
import gh.marad.chi.core.Group
import gh.marad.chi.core.IfElse
import gh.marad.chi.core.WhileLoop
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.asLocation
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser2.*

fun convertGroup(ctx: ConversionContext, ast: ParseGroup): Expression =
    Group(
        value = convert(ctx, ast.value),
        location = ast.section.asLocation()
    )

fun convertIfElse(ctx: ConversionContext, ast: ParseIfElse): Expression =
    IfElse(
        condition = convert(ctx, ast.condition),
        thenBranch = convert(ctx, ast.thenBody),
        elseBranch = ast.elseBody?.let { convert(ctx, it) },
        location = ast.section.asLocation()
    )

fun convertWhen(ctx: ConversionContext, ast: ParseWhen): Expression {
    val lastCase = ast.cases.last()
    val lastCaseAndElse = IfElse(
        condition = convert(ctx, lastCase.condition),
        thenBranch = convert(ctx, lastCase.body),
        elseBranch = ast.elseCase?.body?.let { convert(ctx, it) },
        location = lastCase.section.asLocation()
    )

    return ast.cases.dropLast(1).foldRight<ParseWhenCase, Expression>(lastCaseAndElse) { case, acc ->
        IfElse(
            condition = convert(ctx, case.condition),
            thenBranch = convert(ctx, case.body),
            elseBranch = acc,
            location = case.section.asLocation()
        )
    }
}

fun convertWhile(ctx: ConversionContext, ast: ParseWhile): Expression =
    WhileLoop(
        condition = convert(ctx, ast.condition),
        loop = convert(ctx, ast.body),
        location = ast.section.asLocation()
    )
