package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import gh.marad.chi.core.parser.readers.CommonReader.readTypeParameters

internal object VariantTypeDefinitionReader {
    fun read(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.VariantTypeDefinitionContext
    ): ParseVariantTypeDefinition =
        if (ctx.fullVariantTypeDefinition() != null)
            readFullDefinition(parser, source, ctx.fullVariantTypeDefinition())
        else if (ctx.simplifiedVariantTypeDefinition() != null)
            readSimplifiedDefinition(parser, source, ctx.simplifiedVariantTypeDefinition())
        else TODO("Unsupported type definition syntax!")

    private fun readSimplifiedDefinition(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.SimplifiedVariantTypeDefinitionContext
    ) =
        ParseVariantTypeDefinition(
            typeName = ctx.typeName.text,
            typeParameters = readTypeParameters(source, ctx.generic_type_definitions()),
            variantConstructors = listOf(
                ParseVariantTypeDefinition.Constructor(
                    public = ctx.PUB() != null,
                    name = ctx.typeName.text,
                    formalFields = readFields(parser, source, ctx.variantFields()),
                    section = getSection(source, ctx)
                )
            ),
            section = getSection(source, ctx)
        )

    fun readFullDefinition(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.FullVariantTypeDefinitionContext) =
        ParseVariantTypeDefinition(
            typeName = ctx.typeName.text,
            typeParameters = readTypeParameters(source, ctx.generic_type_definitions()),
            variantConstructors = readConstructors(parser, source, ctx.variantTypeConstructors()),
            section = getSection(source, ctx)
        )

    private fun readConstructors(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.VariantTypeConstructorsContext
    ): List<ParseVariantTypeDefinition.Constructor> =
        ctx.variantTypeConstructor().map {
            ParseVariantTypeDefinition.Constructor(
                public = it.PUB() != null,
                name = it.variantName.text,
                formalFields = readFields(parser, source, it.variantFields()),
                getSection(source, it)
            )
        }

    private fun readFields(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.VariantFieldsContext?
    ): List<FormalField> {
        return ctx?.let {
            it.variantField().map { fieldContext ->
                readField(parser, source, fieldContext)
            }
        } ?: emptyList()
    }

    private fun readField(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.VariantFieldContext): FormalField {
//        TODO()
        return FormalField(
            public = ctx.PUB() != null,
            name = ctx.name.text,
            typeRef = TypeReader.readTypeRef(parser, source, ctx.type()),
            section = getSection(source, ctx)
        )
    }
}

data class ParseVariantTypeDefinition(
    val typeName: String,
    val typeParameters: List<TypeParameter>,
    val variantConstructors: List<Constructor>,
    override val section: ChiSource.Section?
) : ParseAst {
    data class Constructor(
        val public: Boolean,
        val name: String,
        val formalFields: List<FormalField>,
        val section: ChiSource.Section?
    )
}

data class FormalField(val public: Boolean, val name: String, val typeRef: TypeRef, val section: ChiSource.Section?)
