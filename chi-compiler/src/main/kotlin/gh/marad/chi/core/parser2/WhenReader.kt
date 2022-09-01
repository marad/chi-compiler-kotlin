package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object WhenReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.WhenExpressionContext): ParseAst =
        ParseWhen(
            cases = ctx.whenConditionCase().map { readWhenConditionCase(parser, source, it) },
            elseCase = readElseCase(parser, source, ctx.whenElseCase()),
            section = getSection(source, ctx)
        )

    private fun readWhenConditionCase(
        parser: ParserV2,
        source: ChiSource,
        ctx: ChiParser.WhenConditionCaseContext
    ): ParseWhenCase =
        ParseWhenCase(
            condition = ctx.condition.accept(parser),
            body = ctx.body.accept(parser),
            section = getSection(source, ctx)
        )

    private fun readElseCase(
        parser: ParserV2,
        source: ChiSource,
        whenElseCase: ChiParser.WhenElseCaseContext?
    ): ParseElseCase? = whenElseCase?.let {
        ParseElseCase(
            body = it.body.accept(parser),
            section = getSection(source, it)
        )
    }

}

data class ParseWhen(
    val cases: List<ParseWhenCase>,
    val elseCase: ParseElseCase?,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseWhenCase(
    val condition: ParseAst,
    val body: ParseAst,
    val section: ChiSource.Section?
)

data class ParseElseCase(
    val body: ParseAst,
    val section: ChiSource.Section?
)
