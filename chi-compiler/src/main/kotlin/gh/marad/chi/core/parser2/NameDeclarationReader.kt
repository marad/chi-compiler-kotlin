package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2
import gh.marad.chi.core.parser2.TypeReader.readTypeRef

internal object NameDeclarationReader {
    fun read(
        parser: ParserV2,
        source: ChiSource,
        ctx: ChiParser.Name_declarationContext
    ): ParseNameDeclaration =
        ParseNameDeclaration(
            name = CommonReader.readSymbol(source, ctx.ID()),
            typeRef = ctx.type()?.let { readTypeRef(parser, source, it) },
            value = ctx.expression().accept(parser),
            section = getSection(source, ctx)
        )

}

data class ParseNameDeclaration(
    val name: Symbol,
    val typeRef: TypeRef?,
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst
