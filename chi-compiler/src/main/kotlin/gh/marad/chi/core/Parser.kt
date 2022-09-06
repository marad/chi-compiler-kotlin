package gh.marad.chi.core

import ChiLexer
import ChiParser
import gh.marad.chi.core.astconverter.convertProgram
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.ParserVisitor
import gh.marad.chi.core.parser.ProgramReader
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.DefaultErrorStrategy

internal fun parseProgram(source: String, namespace: GlobalCompilationNamespace): Pair<Program, List<Message>> {
    val errorListener = MessageCollectingErrorListener()
    val charStream = CharStreams.fromString(source)
    val lexer = ChiLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(errorListener)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    parser.errorHandler = DefaultErrorStrategy()
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)
    val chiSource = ChiSource(source)
    val visitor = ParserVisitor(chiSource)
    val parsedProgram = ProgramReader.read(visitor, chiSource, parser.program())
    val program = if (errorListener.getMessages().isNotEmpty()) {
        Program(emptyList())
    } else {
        val block = convertProgram(parsedProgram, namespace)
        Program(block.body)
    }
    return Pair(
        automaticallyCastCompatibleTypes(program) as Program,
        errorListener.getMessages()
    )
}
