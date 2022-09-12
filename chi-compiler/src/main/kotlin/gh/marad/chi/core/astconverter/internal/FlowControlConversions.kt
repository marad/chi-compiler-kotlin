package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.Expression
import gh.marad.chi.core.Group
import gh.marad.chi.core.IfElse
import gh.marad.chi.core.WhileLoop
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser.readers.*

fun convertGroup(ctx: ConversionContext, ast: ParseGroup): Expression =
    Group(
        value = convert(ctx, ast.value),
        sourceSection = ast.section
    )

fun convertIfElse(ctx: ConversionContext, ast: ParseIfElse): Expression {
    val ifReading = ConversionContext.IfReadingContext(
        thenScope = ctx.subScope(),
        elseScope = ctx.subScope(),
    )
    return ctx.withIfReadingContext(ifReading) {
        IfElse(
            condition = convert(ctx, ast.condition),
            thenBranch = ctx.withScope(ifReading.thenScope) { convert(ctx, ast.thenBody) },
            elseBranch = ctx.withScope(ifReading.elseScope) { ast.elseBody?.let { convert(ctx, it) } },
            sourceSection = ast.section
        )
    }
}

fun convertWhen(ctx: ConversionContext, ast: ParseWhen): Expression {
    val lastCase = ast.cases.last()
    val lastCaseAndElse = IfElse(
        condition = convert(ctx, lastCase.condition),
        thenBranch = convert(ctx, lastCase.body),
        elseBranch = ast.elseCase?.body?.let { convert(ctx, it) },
        sourceSection = lastCase.section
    )

    return ast.cases.dropLast(1).foldRight<ParseWhenCase, Expression>(lastCaseAndElse) { case, acc ->
        IfElse(
            condition = convert(ctx, case.condition),
            thenBranch = convert(ctx, case.body),
            elseBranch = acc,
            sourceSection = case.section
        )
    }
}

fun convertWhile(ctx: ConversionContext, ast: ParseWhile): Expression =
    WhileLoop(
        condition = convert(ctx, ast.condition),
        loop = convert(ctx, ast.body),
        sourceSection = ast.section
    )
