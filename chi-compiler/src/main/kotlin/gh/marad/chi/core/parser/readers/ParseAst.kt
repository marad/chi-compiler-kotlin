package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.parser.ChiSource

sealed interface ParseAst {
    val section: ChiSource.Section?
    fun children(): List<ParseAst>
}

fun traverseAst(ast: ParseAst, f: (ParseAst) -> Unit) {
    ast.children().forEach { traverseAst(it, f) }
    f(ast)
}

data class ModuleName(val name: String, val section: ChiSource.Section?)
data class PackageName(val name: String, val section: ChiSource.Section?)
data class Symbol(val name: String, val section: ChiSource.Section?)


data class FormalArgument(val name: String, val typeRef: TypeRef, val section: ChiSource.Section?)

data class ParseCast(val value: ParseAst, val typeRef: TypeRef, override val section: ChiSource.Section?) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class ParseWhile(val condition: ParseAst, val body: ParseAst, override val section: ChiSource.Section?) :
    ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class ParseBreak(override val section: ChiSource.Section?) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class ParseContinue(override val section: ChiSource.Section?) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class ParseIs(val value: ParseAst, val typeName: String, override val section: ChiSource.Section?) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}