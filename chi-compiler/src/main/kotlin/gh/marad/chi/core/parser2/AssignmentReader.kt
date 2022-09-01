package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object AssignmentReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.AssignmentContext): ParseAst =
        ParseAssignment(
            variableName = ctx.ID().text,
            index = null,
            value = ctx.value.accept(parser),
            section = getSection(source, ctx)
        )

    fun readIndexed(parser: ParserV2, source: ChiSource, ctx: ChiParser.IndexedAssignmentContext): ParseAst =
        ParseAssignment(
            variableName = ctx.variable.text,
            index = ctx.index.accept(parser),
            value = ctx.value.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseAssignment(
    val variableName: String,
    val index: ParseAst?,
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst