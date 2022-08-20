package gh.marad.chi.core.parser

import ChiParser
import gh.marad.chi.core.GenericTypeParameter

object GenericsReader {
    fun readGenericTypeParameterDefinitions(ctx: ChiParser.Generic_type_definitionsContext?): List<GenericTypeParameter> {
        return ctx?.ID()?.map {
            GenericTypeParameter(it.text)
        } ?: emptyList()
    }
}