package gh.marad.chi.core.parser2

import ChiLexer
import org.antlr.v4.runtime.tree.TerminalNode

object TerminalReader {
    fun read(source: ChiSource, node: TerminalNode): ParseAst {
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> readNumber(source, node)
            else ->
                TODO("Unsupported type ${node.symbol.type}: '${node.symbol.text}'")
        }
    }

    private fun readNumber(source: ChiSource, node: TerminalNode): ParseAst =
        IntValue(node.text.toInt(), getSection(source, node.symbol, node.symbol))
}