package gh.marad.chi.core.parser.readers

import ChiLexer
import ChiParser
import ChiParser.StringPartContext
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import gh.marad.chi.core.parser.mergeSections
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

        var sb = StringBuilder()
        val parts = mutableListOf<StringPart>()
        var currentSection: ChiSource.Section? = null

        fun updateSection(ctx: StringPartContext) {
            val lastSection = currentSection
            val section = getSection(source, ctx)
            currentSection =
                if (lastSection == null) section
                else mergeSections(lastSection, section)
        }

        fun appendTextBeingBuilt() {
            parts.add(StringText(sb.toString(), currentSection))
            sb = StringBuilder()
            currentSection = null
        }

        ctx.stringPart().forEach { part ->
            updateSection(part)
            when {
                part.ID_INTERP() != null -> {
                    val idTerminal = part.ID_INTERP()
                    val variableName = idTerminal.text.drop(1) // drop the  '$' sign at the beginning
                    val value =
                        ParseVariableRead(variableName, getSection(source, idTerminal.symbol, idTerminal.symbol))

                    appendTextBeingBuilt()
                    parts.add(ParseInterpolation(value, getSection(source, part)))
                }
                part.ENTER_EXPR() != null -> {
                    val value = part.expression().accept(parser)
                    appendTextBeingBuilt()
                    parts.add(ParseInterpolation(value, getSection(source, part)))
                }
                part.TEXT() != null -> sb.append(part.TEXT().text)
                part.ESCAPED_DOLLAR() != null -> sb.append("$")
                part.ESCAPED_QUOTE() != null -> sb.append("\"")
                else -> TODO("Unsupported string part!")
            }
        }

        if (sb.isNotEmpty()) {
            parts.add(StringText(sb.toString(), currentSection))
        }

        val withoutEmptyParts = parts.filter { !(it is StringText && it.text.isEmpty()) }

        val singlePart = withoutEmptyParts.singleOrNull()
        return when {
            parts.isEmpty() -> StringValue("", getSection(source, ctx))
            singlePart != null && singlePart is StringText -> {
                StringValue(singlePart.text, getSection(source, ctx))
            }
            else -> {
                ParseInterpolatedString(withoutEmptyParts, getSection(source, ctx))
            }
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

data class ParseInterpolation(val value: ParseAst, override val section: ChiSource.Section?) : StringPart {
    override fun children(): List<ParseAst> = listOf(value)
}

data class ParseInterpolatedString(
    val parts: List<StringPart>,
    override val section: ChiSource.Section?
) : ParseAst {
    override fun children(): List<ParseAst> = parts

}
