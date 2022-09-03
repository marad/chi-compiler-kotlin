package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object FuncReader {
    fun readFunc(parser: ParserV2, source: ChiSource, ctx: ChiParser.FuncContext): ParseAst =
        ParseFunc(
            formalParameters = CommonReader.readFuncArgumentDefinitions(
                parser,
                source,
                ctx.func_argument_definitions()
            ),
            returnTypeRef = TypeReader.readTypeRef(parser, source, ctx.func_return_type().type()),
            body = ctx.func_body().block().accept(parser),
            section = getSection(source, ctx)
        )

    fun readFuncWithName(parser: ParserV2, source: ChiSource, ctx: ChiParser.Func_with_nameContext): ParseAst =
        ParseFuncWithName(
            name = ctx.funcName.text,
            typeParameters = CommonReader.readTypeParameters(source, ctx.generic_type_definitions()),
            formalParameters = CommonReader.readFuncArgumentDefinitions(parser, source, ctx.arguments),
            returnTypeRef = ctx.func_return_type()?.type()
                ?.let { TypeReader.readTypeRef(parser, source, it) },
            body = ctx.func_body().block().accept(parser),
            section = getSection(source, ctx)
        )

    fun readFnCall(parser: ParserV2, source: ChiSource, ctx: ChiParser.FnCallExprContext): ParseAst =
        ParseFnCall(
            function = ctx.expression().accept(parser),
            concreteTypeParameters = ctx.callGenericParameters()?.type()
                ?.map { TypeReader.readTypeRef(parser, source, it) } ?: emptyList(),
            arguments = ctx.expr_comma_list().expression().map { it.accept(parser) },
            section = getSection(source, ctx)
        )

}

data class ParseFunc(
    val formalParameters: List<FormalParameter>,
    val returnTypeRef: TypeRef,
    val body: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseFuncWithName(
    val name: String,
    val typeParameters: List<TypeParameter>,
    val formalParameters: List<FormalParameter>,
    val returnTypeRef: TypeRef?,
    val body: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseFnCall(
    val function: ParseAst,
    val concreteTypeParameters: List<TypeRef>,
    val arguments: List<ParseAst>,
    override val section: ChiSource.Section?,
) : ParseAst