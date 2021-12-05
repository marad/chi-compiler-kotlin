package gh.marad.chi.tac

import gh.marad.chi.core.Type

fun makeFunctionName(name: String, paramTypes: List<Type>): String {
    return if (name != "main") {
        val argParamTypes = paramTypes.joinToString("_") { it.name }
        "${name}_${argParamTypes}"
    } else name
}

