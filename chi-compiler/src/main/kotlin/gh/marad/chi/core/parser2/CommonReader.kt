package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ChiSource

object CommonReader {
    fun readModuleName(source: ChiSource, ctx: ChiParser.Module_nameContext?): ModuleName =
        ModuleName(ctx?.text ?: "", ctx?.let { getSection(source, ctx) })

    fun readPackageName(source: ChiSource, ctx: ChiParser.Package_nameContext?): PackageName =
        PackageName(ctx?.text ?: "", ctx?.let { getSection(source, ctx) })
}