package gh.marad.chi.core.parser

import ChiParser
import gh.marad.chi.core.Expression
import gh.marad.chi.core.IfElse

object MatchReader {
    fun read(context: ParsingContext, ctx: ChiParser.MatchExpressionContext): Expression {
        val iter = ctx.matchCase().iterator()
        return readCase(context, iter)
    }

    private fun readCase(context: ParsingContext, caseIterator: Iterator<ChiParser.MatchCaseContext>): Expression {
        val ctx = caseIterator.next()
        return if (ctx.condition != null) {
            val condition = ctx.condition.accept(context.visitor)
            val thenBranch = ctx.body.accept(context.visitor)
            val elseBranch = if (caseIterator.hasNext()) {
                readCase(context, caseIterator)
            } else {
                null
            }
            IfElse(condition, thenBranch, elseBranch, makeLocation(ctx))
        } else {
            ctx.body.accept(context.visitor)
        }
    }

}