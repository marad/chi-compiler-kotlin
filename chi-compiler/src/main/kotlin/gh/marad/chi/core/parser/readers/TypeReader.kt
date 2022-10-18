package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection
import java.util.*

internal object TypeReader {

    fun readTypeRef(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.TypeContext): TypeRef {
        return if (ctx.typeNameRef() != null) {
            readTypeName(source, ctx.typeNameRef())
        } else if (ctx.functionTypeRef() != null) {
            readFunctionType(parser, source, ctx.functionTypeRef())
        } else if (ctx.typeConstructorRef() != null) {
            readGenericType(parser, source, ctx.typeConstructorRef())
        } else {
            TODO("Unexpected type at ${getSection(source, ctx)}")
        }
    }

    private fun readTypeName(source: ChiSource, ctx: ChiParser.TypeNameRefContext): TypeNameRef {
        if (ctx.packageName != null) {
            TODO("Resolving type from package is not supported.")
        }
        return TypeNameRef(
            typeName = ctx.name.text,
            getSection(source, ctx)
        )
    }

    private fun readFunctionType(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.FunctionTypeRefContext
    ): TypeRef {
        val argTypes = ctx.type().map { readTypeRef(parser, source, it) }
        val returnType = readTypeRef(parser, source, ctx.func_return_type().type())
        return FunctionTypeRef(emptyList(), argTypes, returnType, getSection(source, ctx))
    }

    private fun readGenericType(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.TypeConstructorRefContext
    ): TypeRef {
        val typeName = readTypeName(source, ctx.typeNameRef())
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

data class TypeParameterRef(val name: String, val section: ChiSource.Section?) : TypeRef
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
    val variantFields: List<FormalField>,
    val section: ChiSource.Section?
) : TypeRef {
    override fun equals(other: Any?): Boolean =
        other != null && other is VariantNameRef
                && variantType == other.variantType
                && variantFields == other.variantFields
                && variantName == other.variantName

    override fun hashCode(): Int = Objects.hash(variantType, variantFields, variantName)
}