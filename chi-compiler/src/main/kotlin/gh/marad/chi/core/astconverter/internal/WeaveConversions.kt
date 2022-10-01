package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.Block
import gh.marad.chi.core.Expression
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.ParseWeave
import gh.marad.chi.core.parser.readers.ParseWeavePlaceholder

fun convertWeave(ctx: ConversionContext, weave: ParseWeave): Expression {
    val inputValue = convert(ctx, weave.value)
    val tempVarName = ctx.nextTempVarName()
    val tempVariableDeclaration = NameDeclaration(
        enclosingScope = ctx.currentScope,
        public = false,
        name = tempVarName,
        value = inputValue,
        mutable = false,
        expectedType = null,
        sourceSection = weave.value.section
    )
    ctx.currentScope.addSymbol(tempVarName, tempVariableDeclaration.type, SymbolType.Local, false)
    val readVariable =
        VariableAccess(
            ctx.currentModule, ctx.currentPackage, ctx.currentScope, tempVarName,
            isModuleLocal = true,
            weave.value.section
        )
    val filledTemplate = ctx.withWeaveInput(readVariable) {
        convert(ctx, weave.opTemplate)
    }
    return Block(
        listOf(tempVariableDeclaration, filledTemplate),
        weave.section
    )
}

fun convertWeavePlaceholder(ctx: ConversionContext, placeholder: ParseWeavePlaceholder): Expression {
    return ctx.currentWeaveInput ?: TODO("This should never happen!")
}
