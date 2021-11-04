package gh.marad.chi.core

import gh.marad.chi.core.analyzer.Scope
import gh.marad.chi.core.analyzer.checkTypes

enum class Level { WARNING, ERROR }

sealed interface Message {
    val level: Level
    val message: String
}

data class TypeMismatch(val expected: Type, val actual: Type, val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Expected type is '${expected.name}' but got '${actual.name}'"
}

data class MissingReturnValue(val expectedType: Type, val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Missing return value at ${location?.formattedPosition}"
}

data class FunctionArityError(val functionName: String, val expectedCount: Int, val actualCount: Int, val location: Location?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String = "Function $functionName requires $expectedCount parameters, but was called with $actualCount at ${location?.formattedPosition}"
}

// Rzeczy do sprawdzenia
// - Prosta zgodność typów wyrażeń
// - Nieużywane zmienne
// - Redeklaracja zmiennych (drugie zapisanie var/val w tym samym scope - ale pozwala na shadowing)
// - Weryfikacja istnienia wywoływanych funkcji
// - Weryfikacja istnienia używanych zmiennych
// - Obecność funkcji `main` bez parametrów (później trzeba będzie ogarnąć listę argumentów)

fun analyze(scope: Scope, exprs: List<Expression>): List<Message> {
    return exprs.flatMap { analyze(scope, it) }
}

fun analyze(scope: Scope, expr: Expression): List<Message> {
    // TODO: pozostałe checki
    // Chyba poprawność wywołań i obecność zmiennych w odpowiednich miejscach powinna być przed sprawdzaniem typów.
    // W przeciwnym wypadku wyznaczanie typów wyrażeń może się nie udać
    return checkTypes(scope, expr)
}
