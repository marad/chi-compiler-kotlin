package gh.marad.chi.core.parser2

import ChiLexer
import ChiParser
import org.antlr.v4.runtime.tree.TerminalNode

internal object AtomReader {
    fun readTerminal(source: ChiSource, node: TerminalNode): ParseAst? {
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> readNumber(source, node)
            ChiLexer.NEWLINE -> null
            ChiLexer.TRUE -> BoolValue(true, getSection(source, node.symbol, node.symbol))
            ChiLexer.FALSE -> BoolValue(false, getSection(source, node.symbol, node.symbol))
            ChiLexer.ID -> VariableReader.readVariable(source, node)
            else ->
                TODO("Unsupported type ${node.symbol.type}: '${node.symbol.text}'")
        }
    }

    private fun readNumber(source: ChiSource, node: TerminalNode): ParseAst =
        if (node.text.contains(".")) {
            FloatValue(node.text.toFloat(), getSection(source, node.symbol, node.symbol))
        } else {
            LongValue(node.text.toLong(), getSection(source, node.symbol, node.symbol))
        }

    fun readString(source: ChiSource, ctx: ChiParser.StringContext): ParseAst =
        StringValue(ctx.string_part().joinToString { it.text }, getSection(source, ctx))
}

data class LongValue(val value: Long, override val section: ChiSource.Section? = null) : ParseAst
data class FloatValue(val value: Float, override val section: ChiSource.Section? = null) : ParseAst
data class BoolValue(val value: Boolean, override val section: ChiSource.Section? = null) : ParseAst
data class StringValue(val value: String, override val section: ChiSource.Section? = null) : ParseAst
