package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.parser2.CommonReader.readModuleName
import gh.marad.chi.core.parser2.CommonReader.readPackageName

object ImportReader {
    fun read(source: ChiSource, ctx: ChiParser.Import_definitionContext): ParseImportDefinition =
        ParseImportDefinition(
            moduleName = readModuleName(source, ctx.module_name()),
            packageName = readPackageName(source, ctx.package_name()),
            packageAlias = readPackageAlias(source, ctx.package_import_alias()),
            entries = ctx.import_entry().map { readImportEntry(source, it) },
            section = getSection(source, ctx)
        )

    private fun readPackageAlias(source: ChiSource, ctx: ChiParser.Package_import_aliasContext?): Alias? =
        ctx?.let { Alias(it.text, getSection(source, it)) }

    private fun readImportEntry(source: ChiSource, ctx: ChiParser.Import_entryContext): ParseImportDefinition.Entry =
        ParseImportDefinition.Entry(
            name = ctx.import_name().text,
            alias = readImportNameAlias(source, ctx.name_import_alias()),
            section = getSection(source, ctx)
        )

    private fun readImportNameAlias(source: ChiSource, ctx: ChiParser.Name_import_aliasContext?): Alias? =
        ctx?.let { Alias(it.text, getSection(source, it)) }

}