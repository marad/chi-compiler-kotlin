package gh.marad.chi.core

import ChiParser
import ChiParserBaseVisitor
import gh.marad.chi.core.parser2.*

class ChiSource(val code: String) {
    fun getSection(startIndex: Int, endIndex: Int): Section = Section(this, startIndex, endIndex)

    class Section(val source: ChiSource, val start: Int, val end: Int) {
        fun getCode(): String = source.code.substring(start, end + 1)
    }
}

internal class ParserV2(private val source: ChiSource) : ChiParserBaseVisitor<ParseAst>() {

    override fun visitProgram(ctx: ChiParser.ProgramContext): ParseAst {
        ctx.removeLastChild() // remove EOF
        val body = ctx.children.mapNotNull { it.accept(this) }
        return ParseBlock(body, getSection(source, ctx))
    }

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): ParseAst {
        val moduleName = CommonReader.readModuleName(source, ctx.module_name())
        val packageName = CommonReader.readPackageName(source, ctx.package_name())
        return ParsePackageDefinition(moduleName, packageName, getSection(source, ctx))
    }

    override fun visitImport_definition(ctx: ChiParser.Import_definitionContext): ParseAst {
        return ImportReader.read(source, ctx)
    }

    override fun visitVariantTypeDefinition(ctx: ChiParser.VariantTypeDefinitionContext): ParseAst {
        return VariantTypeDefinitionReader.read(source, ctx)
    }
}