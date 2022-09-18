package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.parser.readers.*
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.traverse.TopologicalOrderIterator

object PackageAstSorter {

    fun sortAsts(asts: List<ParseAst>): List<ParseAst> {
        val namedBlocks = buildNamedBlocks(asts)
        val graph = namedBlocks.toGraph()
        val result = mutableListOf<ParseAst>()
        val iterator = TopologicalOrderIterator(graph)
        iterator.forEach {
            result.addAll(namedBlocks[it]!!.asts)
        }
        return result
    }


    private fun buildNamedBlocks(asts: List<ParseAst>): Map<String, NamedBlockInfo> {
        val result = mutableMapOf<String, NamedBlockInfo>()
        var currentName: String = "@start"
        var currentBlock = mutableListOf<ParseAst>()
        asts.forEach {
            val name = findName(it)
            if (name == null) {
                currentBlock.add(it)
            } else {
                result[currentName] = NamedBlockInfo(
                    asts = currentBlock,
                    usedSymbols = currentBlock.flatMap { ast -> findUsedSymbols(ast) }.toSet()
                )
                currentName = name
                currentBlock = mutableListOf(it)
            }
        }

        result[currentName] = NamedBlockInfo(
            asts = currentBlock,
            usedSymbols = currentBlock.flatMap { ast -> findUsedSymbols(ast) }.toSet()
        )
        return result
    }

    private fun findName(ast: ParseAst): String? = when (ast) {
        is ParseNameDeclaration -> ast.name.name
        is ParseFuncWithName -> ast.name
        else -> null
    }

    private fun findUsedSymbols(ast: ParseAst): Set<String> {
        val names = mutableSetOf<String>()
        traverseAst(ast) {
            if (it is ParseVariableRead) {
                names.add(it.variableName)
            }
            if (it is ParseAssignment) {
                names.add(it.variableName)
            }
        }
        return names
    }

    private fun Map<String, NamedBlockInfo>.toGraph(): DirectedAcyclicGraph<String, DefaultEdge> {
        val graph = DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge::class.java)
        keys.forEach { graph.addVertex(it) }
        val vertices = graph.vertexSet()
        forEach { (name, info) ->
            (info.usedSymbols - name).intersect(vertices)
                .forEach { symbol ->
                    graph.addEdge(symbol, name)
                }
        }
        return graph
    }

    private data class NamedBlockInfo(val asts: List<ParseAst>, val usedSymbols: Set<String>)
}