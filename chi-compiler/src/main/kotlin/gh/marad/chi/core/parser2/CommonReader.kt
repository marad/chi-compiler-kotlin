package gh.marad.chi.core.parser2

import ChiParser
import gh.marad.chi.core.ParserV2
import gh.marad.chi.core.parser2.TypeReader.readTypeRef
import org.antlr.v4.runtime.tree.TerminalNode

internal object CommonReader {
    fun readModuleName(source: ChiSource, ctx: ChiParser.Module_nameContext?): ModuleName =
        ModuleName(ctx?.text ?: "", ctx?.let { getSection(source, ctx) })

    fun readPackageName(source: ChiSource, ctx: ChiParser.Package_nameContext?): PackageName =
        PackageName(ctx?.text ?: "", ctx?.let { getSection(source, ctx) })

    fun readSymbol(source: ChiSource, id: TerminalNode): Symbol =
        Symbol(
            name = id.text,
            section = getSection(source, id.symbol, id.symbol)
        )

    fun readTypeParameters(
        source: ChiSource,
        ctx: ChiParser.Generic_type_definitionsContext?
    ): List<TypeParameter> =
        ctx?.ID()?.map { TypeParameter(it.text, getSection(source, it.symbol, it.symbol)) }
            ?: emptyList()


    fun readFuncArgumentDefinitions(
        parser: ParserV2,
        source: ChiSource,
        ctx: ChiParser.Func_argument_definitionsContext?
    ): List<FormalArgument> =
        ctx?.argumentsWithTypes()?.argumentWithType()?.map {
            FormalArgument(
                name = it.ID().text,
                typeRef = readTypeRef(parser, source, it.type()),
                getSection(source, it)
            )
        } ?: emptyList()

}