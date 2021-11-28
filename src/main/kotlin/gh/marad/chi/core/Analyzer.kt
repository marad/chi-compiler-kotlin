package gh.marad.chi.core

import gh.marad.chi.core.analyzer.*

enum class Level { ERROR }

sealed interface Message {
    val level: Level
    val message: String
}

data class SyntaxError(val offendingSymbol: Any?, val location: Location?, val msg: String?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Syntax error at ${location?.formattedPosition}.${if(msg != null) "Error: $msg" else ""}"
}

data class TypeMismatch(val expected: Type, val actual: Type, val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Expected type is '${expected.name}' but got '${actual.name}' at ${location?.formattedPosition}"
}

data class MissingReturnValue(val expectedType: Type, val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Missing return value at ${location?.formattedPosition}"
}

data class NotAFunction(val name: String, val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Name '$name' is not a function at ${location?.formattedPosition}."
}

data class FunctionArityError(val functionName: String, val expectedCount: Int, val actualCount: Int, val location: Location?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String = "Function $functionName requires $expectedCount parameters, but was called with $actualCount at ${location?.formattedPosition}"
}

data class UnrecognizedName(val name: String, val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Name '$name' was not recognized at ${location?.formattedPosition}"
}

data class IfElseBranchesTypeMismatch(val thenBranchType: Type, val elseBranchType: Type) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Types of if-else branches does not match 'then branch' is '$thenBranchType' and 'else branch' is '$elseBranchType'"
}

// Rzeczy do sprawdzenia
// - Prosta zgodność typów wyrażeń
// - Nieużywane zmienne
// - Redeklaracja zmiennych (drugie zapisanie var/val w tym samym scope - ale pozwala na shadowing)
// - Weryfikacja istnienia wywoływanych funkcji
// - Weryfikacja istnienia używanych zmiennych
// - Obecność funkcji `main` bez parametrów (później trzeba będzie ogarnąć listę argumentów)
// - przypisanie unit

fun analyze(exprs: List<Expression>): List<Message> {
    return exprs.flatMap { analyze(it) }
}

fun analyze(expr: Expression): List<Message> {
    // TODO: pozostałe checki
    // Chyba poprawność wywołań i obecność zmiennych w odpowiednich miejscach powinna być przed sprawdzaniem typów.
    // W przeciwnym wypadku wyznaczanie typów wyrażeń może się nie udać
    val messages = mutableListOf<Message>()

    forEachAst(expr) {
        checkThatSymbolNamesAreDefined(it, messages)
        checkThatFunctionHasAReturnValue(it, messages)
        checkThatFunctionCallsReceiveAppropriateCountOfArguments(it, messages)
        checkThatFunctionCallsActuallyCallFunctions(it, messages)
        checkThatIfElseBranchTypesMatch(it, messages)
    }

    checkTypes(expr, messages)
    return messages
}
