package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2

internal object ProgramReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.ProgramContext): Program {
        val split = ctx.expression().groupBy { isFunctionDeclaration(it) }

        return Program(
            packageDefinition = PackageReader.read(source, ctx.package_definition()),
            imports = ctx.import_definition().map { ImportReader.read(source, it) },
            typeDefinitions = ctx.variantTypeDefinition().map {
                VariantTypeDefinitionReader.read(parser, source, it)
            },
            functions = split[true]?.map { it.accept(parser) } ?: emptyList(),
            topLevelCode = split[false]?.map { it.accept(parser) } ?: emptyList(),
            section = getSection(source, ctx)
        )
    }

    private fun isFunctionDeclaration(ctx: ChiParser.ExpressionContext): Boolean =
//        ctx is ChiParser.NameDeclarationExprContext || ctx is ChiParser.FuncWithNameContext
        (ctx is ChiParser.NameDeclarationExprContext
                && ctx.name_declaration().expression() is ChiParser.FuncExprContext)
                || ctx is ChiParser.FuncWithNameContext

}

data class Program(
    val packageDefinition: ParsePackageDefinition?,
    val imports: List<ParseImportDefinition>,
    val typeDefinitions: List<ParseVariantTypeDefinition>,
    val functions: List<ParseAst>,
    val topLevelCode: List<ParseAst>,
    override val section: ChiSource.Section?
) : ParseAst
