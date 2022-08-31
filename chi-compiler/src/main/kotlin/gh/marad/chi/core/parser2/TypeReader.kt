package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object TypeReader {

    fun readTypeRef(parser: ParserV2, source: ChiSource, ctx: ChiParser.TypeContext): TypeRef {
        return if (ctx.ID() != null) {
            TypeNameRef(ctx.ID().text, getSection(source, ctx))
        } else if (ctx.ARROW() != null) {
            readFunctionType(parser, source, ctx)
        } else if (ctx.generic_type() != null) {
            readGenericType(parser, source, ctx.generic_type())
        } else {
            TODO("Unexpected type at ${getSection(source, ctx)}")
        }
    }

    private fun readFunctionType(parser: ParserV2, source: ChiSource, ctx: ChiParser.TypeContext): TypeRef {
        val argTypes = ctx.type().map { readTypeRef(parser, source, it) }
        val returnType = readTypeRef(parser, source, ctx.func_return_type().type())
        return FunctionTypeRef(argTypes, returnType, getSection(source, ctx))
    }

    private fun readGenericType(parser: ParserV2, source: ChiSource, ctx: ChiParser.Generic_typeContext): TypeRef {
        val typeName = ctx.name.text
        val typeParameters = ctx.type().map { readTypeRef(parser, source, it) }
        return GenericTypeRef(
            typeName, typeParameters, getSection(source, ctx)
        )
    }
}