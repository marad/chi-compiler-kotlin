package gh.marad.chi.core.parser2

import ChiLexer
import ChiParser
import gh.marad.chi.core.ParserV2
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

internal object ProgramReader {
    fun read(parser: ParserV2, source: ChiSource, ctx: ChiParser.ProgramContext): Program {
        val split = ctx.expression().groupBy { isFunctionOrVariableDefinition(it) }

        return Program(
            packageDefinition = PackageReader.read(source, ctx.package_definition()),
            imports = ctx.import_definition().map { ImportReader.read(source, it) },
            typeDefinitions = ctx.variantTypeDefinition().map {
                VariantTypeDefinitionReader.read(parser, source, it)
            },
            functionsAndVariables = split[true]?.map { it.accept(parser) } ?: emptyList(),
            topLevelCode = split[false]?.map { it.accept(parser) } ?: emptyList(),
            section = getSection(source, ctx)
        )
    }

    private fun isFunctionOrVariableDefinition(ctx: ChiParser.ExpressionContext): Boolean =
        ctx is ChiParser.NameDeclarationExprContext || ctx is ChiParser.FuncWithNameContext

}

data class Program(
    val packageDefinition: ParsePackageDefinition?,
    val imports: List<ParseImportDefinition>,
    val typeDefinitions: List<ParseVariantTypeDefinition>,
    val functionsAndVariables: List<ParseAst>,
    val topLevelCode: List<ParseAst>,
    override val section: ChiSource.Section?
) : ParseAst

fun parse(code: String): Program {
    val source = ChiSource(code)
    val charStream = CharStreams.fromString(source.code)

    val lexer = ChiLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    val visitor = ParserV2(source)
    return ProgramReader.read(visitor, source, parser.program())
}

fun main() {
    val code = """
        package some.mod/and.pkg
        import hello/world { x, y, z }
        import std/string as str
        
        data Option[T] = Just(value: T) | Nothing
        data Result[V,E] = Ok(value: V) | Err(error: E)
        
        fn hello() { 1 } 
        val x = 10
        
        println("Hello World")
    """.trimIndent()

    val program = parse(code)

    println("Package: ${program.packageDefinition}")
    println("Imports:")
    program.imports.forEach {
        println(" - $it")
    }

    println("Type definitions:")
    program.typeDefinitions.forEach {
        println(" - $it")
    }

    println("Variables and functions:")
    program.functionsAndVariables.forEach {
        println(" - $it")
    }

    println("Top level code:")
    program.topLevelCode.forEach {
        println(" - $it")
    }

}