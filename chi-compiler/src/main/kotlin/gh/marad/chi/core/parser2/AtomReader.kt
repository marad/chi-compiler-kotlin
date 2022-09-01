package gh.marad.chi.core.parser2

import ChiLexer
import ChiParser
import org.antlr.v4.runtime.tree.TerminalNode

internal object AtomReader {
    fun readTerminal(source: ChiSource, node: TerminalNode): ParseAst? {
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> readNumber(source, node)
            ChiLexer.NEWLINE -> null
            ChiLexer.ID -> VariableReader.readVariable(source, node)
            else ->
                TODO("Unsupported type ${node.symbol.type}: '${node.symbol.text}'")
        }
    }

    private fun readNumber(source: ChiSource, node: TerminalNode): ParseAst =
        LongValue(node.text.toLong(), getSection(source, node.symbol, node.symbol))

    fun readString(source: ChiSource, ctx: ChiParser.StringContext): ParseAst =
        StringValue(ctx.string_part().joinToString { it.text }, getSection(source, ctx))
}

data class LongValue(val value: Long, override val section: ChiSource.Section?) : ParseAst
data class StringValue(val value: String, override val section: ChiSource.Section?) : ParseAst
