package gh.marad.chi.core.parser

import ChiParser
import java.util.*

internal object TypeReader {

    fun readTypeRef(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.TypeContext): TypeRef {
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

    private fun readFunctionType(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.TypeContext): TypeRef {
        val argTypes = ctx.type().map { readTypeRef(parser, source, it) }
        val returnType = readTypeRef(parser, source, ctx.func_return_type().type())
        return FunctionTypeRef(emptyList(), argTypes, returnType, getSection(source, ctx))
    }

    private fun readGenericType(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.Generic_typeContext): TypeRef {
        val typeName = TypeNameRef(ctx.name.text, getSection(source, ctx.name, ctx.name))
        val typeParameters = ctx.type().map { readTypeRef(parser, source, it) }
        return TypeConstructorRef(
            typeName, typeParameters, getSection(source, ctx)
        )
    }
}

sealed interface TypeRef {
    companion object {
        val unit = TypeNameRef("unit", null)
    }
}

data class TypeParameter(val name: String, val section: ChiSource.Section?) : TypeRef
data class TypeNameRef(
    val typeName: String,
    val section: ChiSource.Section?
) : TypeRef {
    override fun equals(other: Any?): Boolean = other != null && other is TypeNameRef && typeName == other.typeName
    override fun hashCode(): Int = Objects.hash(typeName)
}

data class FunctionTypeRef(
    val typeParameters: List<TypeRef>,
    val argumentTypeRefs: List<TypeRef>,
    val returnType: TypeRef,
    val section: ChiSource.Section?
) : TypeRef {
    override fun equals(other: Any?): Boolean =
        other != null && other is FunctionTypeRef
                && argumentTypeRefs == other.argumentTypeRefs
                && returnType == other.returnType

    override fun hashCode(): Int = Objects.hash(argumentTypeRefs, returnType)
}

data class TypeConstructorRef(
    val baseType: TypeRef,
    val typeParameters: List<TypeRef>,
    val section: ChiSource.Section?
) : TypeRef {
    override fun equals(other: Any?): Boolean =
        other != null && other is TypeConstructorRef
                && baseType == other.baseType

    override fun hashCode(): Int = Objects.hash(baseType)
}

data class VariantNameRef(
    val variantType: TypeRef,
    val variantName: String,
    val variantFields: List<FormalArgument>,
    val section: ChiSource.Section?
) : TypeRef {
    override fun equals(other: Any?): Boolean =
        other != null && other is VariantNameRef
                && variantType == other.variantType
                && variantFields == other.variantFields
                && variantName == other.variantName

    override fun hashCode(): Int = Objects.hash(variantType, variantFields, variantName)
}