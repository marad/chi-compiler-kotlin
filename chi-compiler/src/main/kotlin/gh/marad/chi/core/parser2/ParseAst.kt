package gh.marad.chi.core.parser2

sealed interface ParseAst {
    val section: ChiSource.Section?
}

data class IntValue(val value: Int, override val section: ChiSource.Section?) : ParseAst

data class ModuleName(val name: String, val section: ChiSource.Section?)
data class PackageName(val name: String, val section: ChiSource.Section?)
data class Symbol(val name: String, val section: ChiSource.Section?)
data class Alias(val alias: String, val section: ChiSource.Section?)


data class ParseBlock(val body: List<ParseAst>, override val section: ChiSource.Section?) : ParseAst

data class ParsePackageDefinition(
    val moduleName: ModuleName, val packageName: PackageName,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseImportDefinition(
    val moduleName: ModuleName, val packageName: PackageName, val packageAlias: Alias?, val entries: List<Entry>,
    override val section: ChiSource.Section?
) : ParseAst {
    data class Entry(val name: String, val alias: Alias?, val section: ChiSource.Section?)
}

data class TypeParameter(val name: String, val section: ChiSource.Section?)
sealed interface TypeRef
data class TypeNameRef(val typeName: String, val section: ChiSource.Section?) : TypeRef
data class FunctionTypeRef(
    val argumentTypeRefs: List<TypeRef>,
    val returnType: TypeRef,
    val section: ChiSource.Section?
) : TypeRef

data class GenericTypeRef(
    val typeName: String,
    val genericTypeParameters: List<TypeRef>,
    val section: ChiSource.Section?
) : TypeRef

data class FormalParameter(val name: String, val typeRef: TypeRef, val section: ChiSource.Section?)

data class VariantTypeDefinition(
    val typeName: String,
    val typeParameters: List<TypeParameter>,
    val variantConstructors: List<Constructor>,
    override val section: ChiSource.Section?
) : ParseAst {
    data class Constructor(
        val name: String,
        val formalParameters: List<FormalParameter>,
        val section: ChiSource.Section?
    )
}

data class ParseNameDeclaration(
    val name: Symbol,
    val typeRef: TypeRef,
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst