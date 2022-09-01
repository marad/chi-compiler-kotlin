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
}

data class ParseFunc(
    val formalParameters: List<FormalParameter>,
    val returnTypeRef: TypeRef,
    val body: ParseAst,
    override val section: ChiSource.Section?
) : ParseAst