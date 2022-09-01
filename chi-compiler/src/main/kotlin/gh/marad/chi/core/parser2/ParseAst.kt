package gh.marad.chi.core.parser2

sealed interface ParseAst {
    val section: ChiSource.Section?
}

data class IntValue(val value: Int, override val section: ChiSource.Section?) : ParseAst

data class ModuleName(val name: String, val section: ChiSource.Section?)
data class PackageName(val name: String, val section: ChiSource.Section?)
data class Symbol(val name: String, val section: ChiSource.Section?)


data class ParseBlock(val body: List<ParseAst>, override val section: ChiSource.Section?) : ParseAst


data class FormalParameter(val name: String, val typeRef: TypeRef, val section: ChiSource.Section?)