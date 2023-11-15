package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.antlr.ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object FuncReader {
    fun readLambda(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.LambdaContext): ParseAst {
        return ParseLambda(
            formalArguments = CommonReader.readFuncArgumentDefinitions(parser, source, ctx.argumentsWithTypes()),
            body = ctx.expression().map { it.accept(parser) },
            section = getSection(source, ctx)
        )
    }

    fun readFuncWithName(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.Func_with_nameContext): ParseAst =
        ParseFuncWithName(
            public = ctx.PUB() != null,
            name = ctx.funcName.text,
            typeParameters = CommonReader.readTypeParameters(source, ctx.generic_type_definitions()),
            formalArguments = CommonReader.readFuncArgumentDefinitions(
                parser,
                source,
                ctx.arguments.argumentsWithTypes()
            ),
            returnTypeRef = ctx.func_return_type()?.type()
                ?.let { TypeReader.readTypeRef(parser, source, it) },
            body = ctx.func_body().block().accept(parser),
            section = getSection(source, ctx)
        )

    fun readFnCall(parser: ParserVisitor, source: ChiSource, ctx: ChiParser.FnCallExprContext): ParseAst =
        ParseFnCall(
            name = ctx.expression().text,
            function = ctx.expression().accept(parser),
            concreteTypeParameters = ctx.callGenericParameters()?.type()
                ?.map { TypeReader.readTypeRef(parser, source, it) } ?: emptyList(),
            arguments = ctx.expr_comma_list().expression().map { it.accept(parser) },
            section = getSection(source, ctx)
        )

}

data class ParseLambda(
    val formalArguments: List<FormalArgument>,
    val body: List<ParseAst>,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseFuncWithName(
    val public: Boolean,
    val name: String,
    val typeParameters: List<TypeParameterRef>,
    val formalArguments: List<FormalArgument>,
    val returnTypeRef: TypeRef?,
    val body: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst

data class ParseFnCall(
    val name: String,
    val function: ParseAst,
    val concreteTypeParameters: List<TypeRef>,
    val arguments: List<ParseAst>,
    override val section: ChiSource.Section?,
) : ParseAst