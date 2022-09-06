package gh.marad.chi.core.parser

object PackageReader {
    fun read(source: ChiSource, ctx: ChiParser.Package_definitionContext): ParsePackageDefinition {
        val moduleName = CommonReader.readModuleName(source, ctx.module_name())
        val packageName = CommonReader.readPackageName(source, ctx.package_name())
        return ParsePackageDefinition(moduleName, packageName, getSection(source, ctx))
    }
}

data class ParsePackageDefinition(
    val moduleName: ModuleName, val packageName: PackageName,
    override val section: ChiSource.Section?
) : ParseAst

