package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ChiSource

object VariantTypeDefinitionReader {
    fun read(source: ChiSource, ctx: ChiParser.VariantTypeDefinitionContext): VariantTypeDefinition =
        VariantTypeDefinition(
            typeName = ctx.typeName.text,
            typeParameters = readTypeParameters(source, ctx.generic_type_definitions()),
            variantConstructors = readConstructors(source, ctx.variantTypeConstructors()),
            section = getSection(source, ctx)
        )

    fun readConstructors(
        source: ChiSource,
        ctx: ChiParser.VariantTypeConstructorsContext
    ): List<VariantTypeDefinition.Constructor> =
        ctx.variantTypeConstructor().map {
            VariantTypeDefinition.Constructor(
                name = it.variantName.text,
                formalParameters = readFuncArgumentDefinitions(source, it.func_argument_definitions()),
                getSection(source, it)
            )
        }

    fun readTypeParameters(
        source: ChiSource,
        ctx: ChiParser.Generic_type_definitionsContext
    ): List<TypeParameter> =
        ctx.ID().map { TypeParameter(it.text, getSection(source, it.symbol, it.symbol)) }


    fun readFuncArgumentDefinitions(
        source: ChiSource,
        ctx: ChiParser.Func_argument_definitionsContext
    ): List<FormalParameter> =
        ctx.argumentsWithTypes().argumentWithType().map {
            FormalParameter(
                name = it.ID().text,
                typeRequirement = readTypeRequirement(source, it.type()),
                getSection(source, it)
            )
        }

    fun readTypeRequirement(source: ChiSource, ctx: ChiParser.TypeContext): TypeRequirement {
        if (ctx.ID() != null) {
            return TypeNameRequirement(ctx.ID().text, getSection(source, ctx))
        } else if (ctx.ARROW() != null) {
            TODO("test function type reading")
        } else if (ctx.generic_type() != null) {
            // TODO test generic type reading
            TODO("test function type reading")
        } else {
            TODO("Unexpected type at ${getSection(source, ctx)}")
        }
    }
}