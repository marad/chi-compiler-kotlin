package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.Expression
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser.readers.ParseWeave
import gh.marad.chi.core.parser.readers.ParseWeavePlaceholder

fun convertWeave(ctx: ConversionContext, weave: ParseWeave): Expression {
    val inputValue = convert(ctx, weave.value)
    return ctx.withWeaveInput(inputValue) {
        convert(ctx, weave.opTemplate)
    }
}

fun convertWeavePlaceholder(ctx: ConversionContext, placeholder: ParseWeavePlaceholder): Expression {
    return ctx.currentWeaveInput ?: TODO("This should never happen!")
}
