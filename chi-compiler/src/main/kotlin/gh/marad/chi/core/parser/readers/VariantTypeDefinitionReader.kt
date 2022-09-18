package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import gh.marad.chi.core.parser.readers.CommonReader.readFuncArgumentDefinitions
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
                    name = ctx.typeName.text,
                    formalArguments = readFuncArgumentDefinitions(
                        parser,
                        source,
                        ctx.func_argument_definitions()?.argumentsWithTypes()
                    ),
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
                name = it.variantName.text,
                formalArguments = readFuncArgumentDefinitions(
                    parser,
                    source,
                    it.func_argument_definitions()?.argumentsWithTypes()
                ),
                getSection(source, it)
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
        val name: String,
        val formalArguments: List<FormalArgument>,
        val section: ChiSource.Section?
    )

    override fun children(): List<ParseAst> = emptyList()
}

