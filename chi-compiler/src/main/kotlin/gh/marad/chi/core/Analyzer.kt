package gh.marad.chi.core

import gh.marad.chi.core.analyzer.*

enum class Level { ERROR }

sealed interface Message {
    val level: Level
    val message: String
    val location: Location?
}

data class SyntaxError(val offendingSymbol: Any?, override val location: Location?, val msg: String?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Syntax error at ${location?.formattedPosition}.${if(msg != null) "Error: $msg" else ""}"
}

data class TypeMismatch(val expected: Type, val actual: Type, override val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Expected type is '${expected.name}' but got '${actual.name}' at ${location?.formattedPosition}"
}

data class MissingReturnValue(val expectedType: Type, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Missing return value at ${location?.formattedPosition}"
}

data class NotAFunction(override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "This is not a function ${location?.formattedPosition}."
}

data class FunctionArityError(val expectedCount: Int, val actualCount: Int, override val location: Location?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String = "Function requires $expectedCount parameters, but was called with $actualCount at ${location?.formattedPosition}"
}

data class NoCandidatesForFunction(val argumentTypes: List<Type>,
                                   override val location: Location?): Message {
    override val level: Level = Level.ERROR
    override val message: String = "No candidates to call for function with arguments ${argumentTypes.map { it.name }}"

}

data class UnrecognizedName(val name: String, override val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Name '$name' was not recognized at ${location?.formattedPosition}"
}

data class IfElseBranchesTypeMismatch(val thenBranchType: Type, val elseBranchType: Type,
                                      override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Types of if-else branches does not match 'then branch' is '$thenBranchType' and 'else branch' is '$elseBranchType'"
}

// Rzeczy do sprawdzenia
// - Prosta zgodno???? typ??w wyra??e??
// - Nieu??ywane zmienne
// - Redeklaracja zmiennych (drugie zapisanie var/val w tym samym scope - ale pozwala na shadowing)
// - Obecno???? funkcji `main` bez parametr??w (p????niej trzeba b??dzie ogarn???? list?? argument??w)
// - przypisanie unit
fun analyze(expr: Expression): List<Message> {
    // TODO: pozosta??e checki
    // Chyba poprawno???? wywo??a?? i obecno???? zmiennych w odpowiednich miejscach powinna by?? przed sprawdzaniem typ??w.
    // W przeciwnym wypadku wyznaczanie typ??w wyra??e?? mo??e si?? nie uda??
    val messages = mutableListOf<Message>()

    forEachAst(expr) {
        checkThatVariableIsDefined(it, messages)
        checkThatFunctionHasAReturnValue(it, messages)
        checkThatFunctionCallsReceiveAppropriateCountOfArguments(it, messages)
        checkForOverloadedFunctionCallCandidate(it, messages)
        checkThatFunctionCallsActuallyCallFunctions(it, messages)
        checkThatIfElseBranchTypesMatch(it, messages)
        checkTypes(it, messages)
    }

    return messages
}
