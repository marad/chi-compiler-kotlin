package gh.marad.chi.core

import gh.marad.chi.core.analyzer.*

enum class Level { ERROR }

sealed interface Message {
    val level: Level
    val message: String
    val location: Location?
}

data class InvalidImport(val details: String?, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = if (details != null) "Invalid import: $details" else "Invalid import"
}

data class InvalidModuleName(val moduleName: String, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Invalid module name '$moduleName' at ${location?.formattedPosition}"
}

data class InvalidPackageName(val packageName: String, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Invalid package name '$packageName' at ${location?.formattedPosition}"
}

data class SyntaxError(val offendingSymbol: Any?, val msg: String?, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Syntax error at ${location?.formattedPosition}.${if (msg != null) "Error: $msg" else ""}"
}

data class TypeMismatch(val expected: Type, val actual: Type, override val location: Location?) : Message {
    override val level = Level.ERROR
    override val message =
        "Expected type is '${expected.name}' but got '${actual.name}' at ${location?.formattedPosition}"
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
    override val message: String =
        "Function requires $expectedCount parameters, but was called with $actualCount at ${location?.formattedPosition}"
}

data class GenericTypeArityError(val expectedCount: Int, val actualCount: Int, override val location: Location?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Function requires $expectedCount generic type parameters, but was called with $actualCount"
}

data class NoCandidatesForFunction(
    val argumentTypes: List<Type>,
    override val location: Location?
) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "No candidates to call for function with arguments ${argumentTypes.map { it.name }}"

}

data class UnrecognizedName(val name: String, override val location: Location?) : Message {
    override val level = Level.ERROR
    override val message = "Name '$name' was not recognized at ${location?.formattedPosition}"
}

data class IfElseBranchesTypeMismatch(
    val thenBranchType: Type, val elseBranchType: Type,
    override val location: Location?
) : Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Types of if-else branches does not match 'then branch' is '$thenBranchType' and 'else branch' is '$elseBranchType'"
}

data class TypeIsNotIndexable(val type: Type, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Type '${type.name}' is cannot be indexed"
}

data class CannotChangeImmutableVariable(override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Cannot change immutable variable"
}

data class MemberDoesNotExist(val type: Type, val member: String, override val location: Location?) : Message {
    override val level: Level = Level.ERROR
    override val message: String
        get() = "Type ${type.name} does not have field '$member', or I don't have enough information about the type variant"
}

// Rzeczy do sprawdzenia
// - Prosta zgodność typów wyrażeń
// - Nieużywane zmienne
// - Redeklaracja zmiennych (drugie zapisanie var/val w tym samym scope - ale pozwala na shadowing)
// - Obecność funkcji `main` bez parametrów (później trzeba będzie ogarnąć listę argumentów)
// - przypisanie unit
fun analyze(expr: Expression): List<Message> {
    // TODO: pozostałe checki
    // Chyba poprawność wywołań i obecność zmiennych w odpowiednich miejscach powinna być przed sprawdzaniem typów.
    // W przeciwnym wypadku wyznaczanie typów wyrażeń może się nie udać
    val messages = mutableListOf<Message>()

    forEachAst(expr) {
        checkModuleAndPackageNames(it, messages)
        checkImports(it, messages)
        checkThatTypesContainAccessedMembers(it, messages)
        checkThatVariableIsDefined(it, messages)
        checkThatFunctionHasAReturnValue(it, messages)
        checkThatFunctionCallsReceiveAppropriateCountOfArguments(it, messages)
        checkForOverloadedFunctionCallCandidate(it, messages)
        checkThatFunctionCallsActuallyCallFunctions(it, messages)
        checkGenericTypes(it, messages)
        checkThatIfElseBranchTypesMatch(it, messages)
        checkTypes(it, messages)
        checkThatAssignmentDoesNotChangeImmutableValue(it, messages)
    }

    return messages
}
