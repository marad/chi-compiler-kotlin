package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.Atom
import gh.marad.chi.core.Expression
import gh.marad.chi.core.Location
import gh.marad.chi.core.LocationPoint
import gh.marad.chi.core.parser2.*

// konwertując blok, zanim zacznę konwersję potrzebuję:
// - zrobić listę zdefiniowanych i zaimportowanych typów
// - zrobić listę zdefiniowanych i zaimportowanych symboli
// dopiero mając taki kontekst mogę konwertować na CoreAst

fun parseAstToExpression(ast: ParseAst): Expression {
    return when (ast) {
        is BoolValue -> if (ast.value) Atom.t(ast.section.asLocation()) else Atom.f(ast.section.asLocation())
        is FloatValue -> Atom.float(ast.value, ast.section.asLocation())
        is LongValue -> Atom.int(ast.value, ast.section.asLocation())
        is StringValue -> Atom.string(ast.value, ast.section.asLocation())
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
        is VariantTypeDefinition -> TODO()
        is ParseCast -> TODO()
    }
}

private fun ChiSource.Section?.asLocation(): Location? {
    return this?.let {
        Location(
            start = LocationPoint(it.startLine, it.startColumn),
            end = LocationPoint(it.endLine, it.endColumn),
            startIndex = it.start,
            endIndex = it.end
        )
    }
}