package gh.marad.chi.core.parser

import ChiParser
import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type

object TypeReader {
    fun read(context: ParsingContext, ctx: ChiParser.TypeContext): Type {
        val primitiveType = ctx.ID()?.let { maybePrimitiveType(it.text) }
        return if (ctx.ID()?.text == "any") {
            Type.any
        } else if (primitiveType != null) {
            primitiveType
        } else if (ctx.ID() != null) {
            readVariantTypeOrGenericTypeParameter(context, ctx)
        } else if (ctx.generic_type() != null) {
            readGenericType(context, ctx)
        } else {
            readFunctionType(context, ctx)
        }
    }

    private fun maybePrimitiveType(name: String): Type? = Type.primitiveTypes.find { it.name == name }

    private fun readVariantTypeOrGenericTypeParameter(
        context: ParsingContext,
        ctx: ChiParser.TypeContext
    ): Type {
        val typeName = ctx.ID().text
        val type = context.currentPackageDescriptor.variantTypes.get(typeName)?.getWithSingleOrNoVariant()
            ?: context.imports.lookupType(typeName)?.getWithSingleOrNoVariant()

        return type ?: Type.typeParameter(ctx.ID().text)
    }

    private fun readGenericType(
        context: ParsingContext,
        ctx: ChiParser.TypeContext
    ): Type {
        val genericTypeName = ctx.generic_type().name.text
        val genericTypeParameters = ctx.generic_type().type().map { read(context, it) }
        val variantType =
            context.currentPackageDescriptor.variantTypes.get(genericTypeName)?.getWithSingleOrNoVariant()
                ?: context.imports.lookupType(genericTypeName)?.getWithSingleOrNoVariant()

        return if (genericTypeName == "array") {
            Type.array(genericTypeParameters.first())
        } else if (variantType != null) {
            val genericParameterTypeMap = variantType.genericTypeParameters.zip(genericTypeParameters).toMap()
            variantType.construct(genericParameterTypeMap)
        } else {
            TODO("Unknown generic type '$genericTypeName' with parameters $genericTypeParameters")
        }
    }

    private fun readFunctionType(
        context: ParsingContext,
        ctx: ChiParser.TypeContext
    ): FnType {
        val argTypes = ctx.type().map { read(context, it) }
        val returnType = read(context, ctx.func_return_type().type())
        return FnType(emptyList(), argTypes, returnType)
    }
}