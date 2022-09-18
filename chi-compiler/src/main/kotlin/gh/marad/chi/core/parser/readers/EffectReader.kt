package gh.marad.chi.core.parser.readers

import ChiParser
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.getSection

internal object EffectReader {
    fun readEffectDefinition(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.EffectDefinitionContext
    ): ParseAst =
        ParseEffectDefinition(
            name = ctx.effectName.text,
            formalArguments = CommonReader.readFuncArgumentDefinitions(
                parser,
                source,
                ctx.arguments?.argumentsWithTypes(),
            ),
            returnTypeRef = TypeReader.readTypeRef(parser, source, ctx.type()),
            section = getSection(source, ctx)
        )

    fun readHandle(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.HandleExpressionContext
    ): ParseAst =
        ParseHandle(
            body = BlockReader.read(parser, source, ctx.block()),
            cases = ctx.handleCase().map { readHandleCase(parser, source, it) },
            section = getSection(source, ctx)
        )

    private fun readHandleCase(
        parser: ParserVisitor,
        source: ChiSource,
        ctx: ChiParser.HandleCaseContext
    ): ParseHandleCase =
        ParseHandleCase(
            effectName = ctx.effectName.text,
            argumentNames = ctx.handleCaseEffectParam().map { it.text },
            body = ctx.handleCaseBody().accept(parser),
            section = getSection(source, ctx)
        )
}

data class ParseEffectDefinition(
    val name: String,
    val formalArguments: List<FormalArgument>,
    val returnTypeRef: TypeRef,
    override val section: ChiSource.Section?
) : ParseAst {
    override fun children(): List<ParseAst> = emptyList()
}

data class ParseHandle(
    val body: ParseBlock,
    val cases: List<ParseHandleCase>,
    override val section: ChiSource.Section?
) : ParseAst {
    override fun children(): List<ParseAst> = body.children() + cases.flatMap { it.children() }
}

data class ParseHandleCase(
    val effectName: String,
    val argumentNames: List<String>,
    val body: ParseAst,
    val section: ChiSource.Section?
) {
    fun children(): List<ParseAst> = listOf(body)
}