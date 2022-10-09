package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.expressionast.generateExpressionAst
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.readers.*

fun convertGroup(ctx: ConversionContext, ast: ParseGroup): Group =
    Group(
        value = generateExpressionAst(ctx, ast.value),
        sourceSection = ast.section
    )

fun convertIfElse(ctx: ConversionContext, ast: ParseIfElse): IfElse {
    return readIfElse(ctx, ast.condition, ast.thenBody, ast.elseBody, ast.section)
}

fun convertWhen(ctx: ConversionContext, ast: ParseWhen): Expression {
    val lastCase = ast.cases.last()
    val lastCaseAndElse = readIfElse(ctx, lastCase.condition, lastCase.body, ast.elseCase?.body, lastCase.section)

    return ast.cases.dropLast(1).foldRight<ParseWhenCase, Expression>(lastCaseAndElse) { case, acc ->
        val ifReading = ConversionContext.IfReadingContext(
            thenScope = ctx.virtualSubscope(),
            elseScope = ctx.virtualSubscope()
        )
        ctx.withIfReadingContext(ifReading) {
            IfElse(
                condition = generateExpressionAst(ctx, case.condition),
                thenBranch = ctx.withScope(ifReading.thenScope) { generateExpressionAst(ctx, case.body) },
                elseBranch = acc,
                sourceSection = case.section
            )
        }
    }
}

private fun readIfElse(
    ctx: ConversionContext,
    condition: ParseAst,
    thenBody: ParseAst,
    elseBody: ParseAst?,
    section: ChiSource.Section?
): IfElse {
    val ifReading = ConversionContext.IfReadingContext(
        thenScope = ctx.virtualSubscope(),
        elseScope = ctx.virtualSubscope(),
    )
    return ctx.withIfReadingContext(ifReading) {
        IfElse(
            condition = generateExpressionAst(ctx, condition),
            thenBranch = ctx.withScope(ifReading.thenScope) { generateExpressionAst(ctx, thenBody) },
            elseBranch = ctx.withScope(ifReading.elseScope) { elseBody?.let { generateExpressionAst(ctx, it) } },
            sourceSection = section
        )
    }
}

fun convertWhile(ctx: ConversionContext, ast: ParseWhile): Expression =
    WhileLoop(
        condition = generateExpressionAst(ctx, ast.condition),
        loop = generateExpressionAst(ctx, ast.body),
        sourceSection = ast.section
    )

fun convertBreak(ast: ParseBreak): Expression =
    Break(ast.section)

fun convertContinue(ast: ParseContinue): Expression =
    Continue(ast.section)
