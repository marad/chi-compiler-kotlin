package gh.marad.chi.core.parser

import ChiLexer
import gh.marad.chi.core.*
import org.antlr.v4.runtime.tree.TerminalNode

object TerminalReader {
    fun read(context: ParsingContext, node: TerminalNode): Expression? {
        val location = makeLocation(node.symbol, node.symbol)
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> readNumber(node, location)
            ChiLexer.ID -> readVariableAccess(context, node, location)
            ChiLexer.TRUE -> Atom.t(location)
            ChiLexer.FALSE -> Atom.f(location)
            ChiLexer.NEWLINE -> null
            else -> {
                TODO("Unsupported type ${node.symbol.type}")
            }
        }
    }

    private fun readNumber(
        node: TerminalNode,
        location: Location
    ) = if (node.text.contains(".")) {
        Atom(node.text, Type.floatType, location)
    } else {
        Atom(node.text, Type.intType, location)
    }

    private fun readVariableAccess(
        context: ParsingContext,
        node: TerminalNode,
        location: Location
    ): VariableAccess {
        val import = context.imports.lookupName(node.text)
        return if (import != null) {
            VariableAccess(
                import.module,
                import.pkg,
                definitionScope = context.namespace.getOrCreatePackage(import.module, import.pkg).scope,
                import.name,
                location
            )
        } else {
            VariableAccess(
                context.currentModule,
                context.currentPackage,
                context.currentScope,
                node.text,
                location
            )
        }
    }
}