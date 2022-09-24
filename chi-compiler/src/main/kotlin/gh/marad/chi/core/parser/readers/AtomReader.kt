package gh.marad.chi.core.parser.readers

import ChiLexer
import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import org.antlr.v4.runtime.tree.TerminalNode

internal object AtomReader {
    fun readTerminal(source: ChiSource, node: TerminalNode): ParseAst? {
        val section = getSection(source, node.symbol, node.symbol)
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> readNumber(source, node)
            ChiLexer.NEWLINE -> null
            ChiLexer.TRUE -> BoolValue(true, section)
            ChiLexer.FALSE -> BoolValue(false, section)
            ChiLexer.ID -> VariableReader.readVariable(source, node)
            ChiLexer.PLACEHOLDER -> ParseWeavePlaceholder(section)
            else ->
                TODO("Unsupported terminal type ${node.symbol.type}: '${node.symbol.text}'")
        }
    }

    private fun readNumber(source: ChiSource, node: TerminalNode): ParseAst =
        if (node.text.contains(".")) {
            FloatValue(node.text.toFloat(), getSection(source, node.symbol, node.symbol))
        } else {
            LongValue(node.text.toLong(), getSection(source, node.symbol, node.symbol))
        }

    fun readString(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.StringContext): ParseAst {
        val section = getSection(source, ctx)
        return if (containsInterpolation(ctx)) {
            val parts = ctx.stringPart().map { readStringPart(parser, source, it) }
            InterpolatedString(parts, section)
        } else {
            StringValue(ctx.stringPart().joinToString(separator = "") {
                when (it.start.type) {
                    ChiLexer.ESCAPED_DOLLAR -> "$"
                    ChiLexer.ESCAPED_QUOTE -> "\""
                    else -> it.text
                }
            }, section)
        }
    }

    private fun containsInterpolation(ctx: ChiParser.StringContext) =
        ctx.stringPart().any { it.ID_INTERP() != null || it.ENTER_EXPR() != null }

    private fun readStringPart(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.StringPartContext): StringPart {
        val section = getSection(source, ctx)
        return when {
            ctx.TEXT() != null -> StringText(ctx.TEXT().text, section)
            ctx.ID_INTERP() != null -> {
                val idTerminal = ctx.ID_INTERP()
                val variableName = idTerminal.text.drop(1) // drop the  '$' sign at the beginning
                val value = ParseVariableRead(variableName, getSection(source, idTerminal.symbol, idTerminal.symbol))
                Interpolation(value, section)
            }
            ctx.ENTER_EXPR() != null -> {
                val value = ctx.expression().accept(parser)
                Interpolation(value, section)
            }
            ctx.ESCAPED_DOLLAR() != null -> StringText("$", section);
            ctx.ESCAPED_QUOTE() != null -> StringText("\"", section);
            else -> TODO("Unsupported string part!")
        }
    }
}

data class LongValue(val value: Long, override val section: ChiSource.Section? = null) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class FloatValue(val value: Float, override val section: ChiSource.Section? = null) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class BoolValue(val value: Boolean, override val section: ChiSource.Section? = null) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class StringValue(val value: String, override val section: ChiSource.Section? = null) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

sealed interface StringPart : ParseAst
data class StringText(val text: String, override val section: ChiSource.Section?) : StringPart {
    override fun children(): List<ParseAst> = emptyList()
}

data class Interpolation(val value: ParseAst, override val section: ChiSource.Section?) : StringPart {
    override fun children(): List<ParseAst> = listOf(value)
}

data class InterpolatedString(
    val parts: List<StringPart>,
    override val section: ChiSource.Section?
) : ParseAst {
    override fun children(): List<ParseAst> = parts

}
