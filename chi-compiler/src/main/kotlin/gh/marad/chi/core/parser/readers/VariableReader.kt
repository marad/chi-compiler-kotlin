package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import org.antlr.v4.runtime.tree.TerminalNode

internal object VariableReader {
    fun readVariable(source: ChiSource, ctx: TerminalNode) =
        ParseVariableRead(
            variableName = ctx.text,
            section = getSection(source, ctx.symbol, ctx.symbol)
        )


    fun readVariableIndexed(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.IndexOperatorContext): ParseAst =
        ParseIndexOperator(
            variable = ctx.variable.accept(parser),
            index = ctx.index.accept(parser),
            section = getSection(source, ctx)
        )

    fun readAssignment(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.AssignmentContext): ParseAst =
        ParseAssignment(
            variableName = ctx.ID().text,
            value = ctx.value.accept(parser),
            section = getSection(source, ctx)
        )

    fun readIndexedAssignment(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.IndexedAssignmentContext
    ): ParseAst =
        ParseIndexedAssignment(
            variable = ctx.variable.accept(parser),
            index = ctx.index.accept(parser),
            value = ctx.value.accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseAssignment(
    val variableName: String,
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseIndexedAssignment(
    val variable: ParseAst,
    val index: ParseAst,
    val value: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst

data class ParseVariableRead(
    val variableName: String,
    override val section: ChiSource.Section? = null
) : ParseAst


data class ParseIndexOperator(
    val variable: ParseAst,
    val index: ParseAst,
    override val section: ChiSource.Section?,
) : ParseAst
