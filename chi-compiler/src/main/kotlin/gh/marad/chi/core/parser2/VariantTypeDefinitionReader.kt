package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2
import gh.marad.chi.core.parser2.CommonReader.readFuncArgumentDefinitions
import gh.marad.chi.core.parser2.CommonReader.readTypeParameters

internal object VariantTypeDefinitionReader {
    fun read(
        parser: ParserV2,
        source: ChiSource,
        ctx: ChiParser.VariantTypeDefinitionContext
    ): ParseVariantTypeDefinition =
        ParseVariantTypeDefinition(
            typeName = ctx.typeName.text,
            typeParameters = readTypeParameters(source, ctx.generic_type_definitions()),
            variantConstructors = readConstructors(parser, source, ctx.variantTypeConstructors()),
            section = getSection(source, ctx)
        )

    private fun readConstructors(
        parser: ParserV2,
        source: ChiSource,
        ctx: ChiParser.VariantTypeConstructorsContext
    ): List<ParseVariantTypeDefinition.Constructor> =
        ctx.variantTypeConstructor().map {
            ParseVariantTypeDefinition.Constructor(
                name = it.variantName.text,
                formalArguments = readFuncArgumentDefinitions(parser, source, it.func_argument_definitions()),
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
}

