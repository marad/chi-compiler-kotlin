package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import gh.marad.chi.core.parser.readers.TypeReader.readTypeRef

internal object NameDeclarationReader {
    fun read(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.Name_declarationContext
    ): ParseNameDeclaration =
        ParseNameDeclaration(
            public = ctx.PUB() != null,
            mutable = ctx.VAR() != null,
            name = CommonReader.readSymbol(source, ctx.ID()),
            typeRef = ctx.type()?.let { readTypeRef(parser, source, it) },
            value = ctx.expression().accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseNameDeclaration(
    val public: Boolean,
    val mutable: Boolean,
    val name: Symbol,
    val typeRef: TypeRef?,
    val value: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst
