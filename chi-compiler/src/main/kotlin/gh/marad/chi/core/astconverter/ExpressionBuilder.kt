package gh.marad.chi.core.astconverter

import ChiLexer
import ChiParser
import gh.marad.chi.core.*
import gh.marad.chi.core.parser.*
import gh.marad.chi.core.parser.Program
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

// konwertując blok, zanim zacznę konwersję potrzebuję:
// - zrobić listę zdefiniowanych i zaimportowanych typów
// - zrobić listę zdefiniowanych i zaimportowanych symboli
// dopiero mając taki kontekst mogę konwertować na CoreAst

//enum class SymbolScope { Local, Argument, Package }
//enum class SymbolKind { Function, Variable }
//data class SymbolInfo(
//    val moduleName: ModuleName,
//    val packageName: PackageName,
//    val name: String,
//    val scope: SymbolScope,
//    val kind: SymbolKind
//)

//class SymbolTable {
//    private val symbols = mutableMapOf<String, SymbolInfo>()
//    private val types =
//
//}


fun parseAstToAtom(value: BoolValue) =
    if (value.value) Atom.t(value.section.asLocation())
    else Atom.f(value.section.asLocation())

fun parseAstToAtom(value: FloatValue) =
    Atom.float(value.value, value.section.asLocation())

fun parseAstToAtom(ast: LongValue) =
    Atom.int(ast.value, ast.section.asLocation())

fun parseAstToAtom(ast: StringValue) =
    Atom.string(ast.value, ast.section.asLocation())

fun parseToCore(ast: ParseAssignment, scope: CompilationScope): Assignment =
    Assignment(scope, ast.variableName, parseToCore(ast.value), ast.section.asLocation())

fun parseToCore(ast: ParseAst): Expression {
    TODO()
}

fun parseAstToExpression(
    ast: ParseAst,
    namespace: GlobalCompilationNamespace = GlobalCompilationNamespace()
): Expression {
    return when (ast) {
        is BoolValue -> parseAstToAtom(ast)
        is FloatValue -> parseAstToAtom(ast)
        is LongValue -> parseAstToAtom(ast)
        is StringValue -> parseAstToAtom(ast)
        is ParseAssignment -> TODO()
        is ParseBinaryOp -> TODO()
        is ParseBlock -> TODO()
        is ParseDotOp -> TODO()
        is ParseFnCall -> TODO()
        is ParseFunc -> TODO()
        is ParseFuncWithName -> TODO()
        is ParseGroup -> TODO()
        is ParseIfElse -> TODO()
        is ParseImportDefinition -> TODO()
        is ParseIndexOperator -> TODO()
        is ParseIs -> TODO()
        is ParseNameDeclaration -> TODO()
        is ParseNot -> TODO()
        is ParsePackageDefinition -> TODO()
        is ParseVariableRead -> TODO()
        is ParseWhen -> TODO()
        is ParseWhile -> TODO()
        is ParseVariantTypeDefinition -> TODO()
        is ParseCast -> TODO()
        is Program -> TODO()
        is ParseIndexedAssignment -> TODO()
    }
}

fun ChiSource.Section?.asLocation(): Location? {
    return this?.let {
        Location(
            start = LocationPoint(it.startLine, it.startColumn),
            end = LocationPoint(it.endLine, it.endColumn),
            startIndex = it.start,
            endIndex = it.end
        )
    }
}

fun parse(code: String): Program {
    val source = ChiSource(code)
    val charStream = CharStreams.fromString(source.code)

    val lexer = ChiLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    val visitor = ParserVisitor(source)
    return ProgramReader.read(visitor, source, parser.program())
}

fun main() {
    val code = """
        package some.mod/and.pkg
        import hello/world { x, y, z }
        import std/string as str
        
        data Option[T] = Just(value: T) | Nothing
        data Result[V,E] = Ok(value: V) | Err(error: E)
        
        fn test[T](t: T): T { t }
        fn hello() { 1 } 
        fn hello(s: string): int { 32 }
        val foo = fn(a: string): int { a + 10 }
        fn zzz[Z](x: Option[Z]): Option[Z] { x }
        fn arr(): array[int] {}
        val x = 10
        
        println("Hello World")
        x = 5
        5 as string
        asd[10] = "hello"
        str.toUpper("hello")
        val qwe = Just(5)
        qwe.value = 10
        qwe.value
        (2 + 3)
        if (true) 1 else 2
        asd[10]
        !hello
        when {
          a == b -> "hello"
          true -> "world"
        }
    """.trimIndent()

    val program = parse(code)
//    val result = FooBar.getFunctionDescriptors(program)
//
//    result.forEach {
//        println("- ${it.name} : ${it.type}")
//    }

    val block = convertProgram(program, GlobalCompilationNamespace())
    block.body.forEach { println(it) }
}
