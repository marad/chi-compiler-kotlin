package gh.marad.chi.core.analyzer

import gh.marad.chi.core.*
import gh.marad.chi.core.parser.ChiSource

enum class Level { ERROR }

data class CodePoint(val line: Int, val column: Int) {
    override fun toString(): String = "$line:$column"
}

fun ChiSource.Section?.toCodePoint(): CodePoint? =
    this?.let { CodePoint(startLine, startColumn) }

sealed interface Message {
    val level: Level
    val message: String
    val codePoint: CodePoint?
}

data class InvalidImport(val details: String?, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = if (details != null) "Invalid import: $details" else "Invalid import"
}

data class InvalidModuleName(val moduleName: String, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Invalid module name '$moduleName' at $codePoint"
}

data class InvalidPackageName(val packageName: String, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Invalid package name '$packageName' at $codePoint"
}

data class SyntaxError(val offendingSymbol: Any?, val msg: String?, override val codePoint: CodePoint?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Syntax error at $codePoint.${if (msg != null) "Error: $msg" else ""}"
}

data class TypeMismatch(val expected: Type, val actual: Type, override val codePoint: CodePoint?) :
    Message {
    override val level = Level.ERROR
    override val message =
        "Expected type is '${expected.toDisplayString()}' but got '${actual.toDisplayString()}' at $codePoint"
}

data class GenericTypeMismatch(
    val expected: Type,
    val actual: Type,
    val genericTypeParameter: GenericTypeParameter,
    override val codePoint: CodePoint?
) : Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Expected type of type parameter '${genericTypeParameter.typeParameterName}' is '${expected.toDisplayString()}' but got '${actual.toDisplayString()}'"
}

data class MissingReturnValue(val expectedType: Type, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Missing return value at $codePoint"
}

data class NotAFunction(override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "This is not a function $codePoint."
}

data class FunctionArityError(
    val expectedCount: Int,
    val actualCount: Int,
    override val codePoint: CodePoint?
) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Function requires $expectedCount parameters, but was called with $actualCount at $codePoint"
}

data class GenericTypeArityError(
    val expectedCount: Int,
    val actualCount: Int,
    override val codePoint: CodePoint?
) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "Function requires $expectedCount generic type parameters, but was called with $actualCount"
}

data class NoCandidatesForFunction(
    val argumentTypes: List<Type>,
    val options: Set<FnType>,
    override val codePoint: CodePoint?
) : Message {
    override val level: Level = Level.ERROR
    override val message: String =
        "No candidates to call for function with arguments ${argumentTypes.map { it.name }}. Options are: ${
            options.map { "(" + it.paramTypes.joinToString(", ") { it.toDisplayString() } + ")" }
        }"

}

data class UnrecognizedName(val name: String, override val codePoint: CodePoint?) : Message {
    override val level = Level.ERROR
    override val message = "Name '$name' was not recognized at $codePoint"
}

data class TypeIsNotIndexable(val type: Type, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Type '${type.name}' is cannot be indexed"
}

data class CannotChangeImmutableVariable(override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String = "Cannot change immutable variable"
}

data class MemberDoesNotExist(val type: Type, val member: String, override val codePoint: CodePoint?) :
    Message {
    override val level: Level = Level.ERROR
    override val message: String
        get() = "Type ${type.name} does not have field '$member', or I don't have enough information about the type variant"
}

data class ExpectedVariantType(val actual: Type, override val codePoint: CodePoint?) : Message {
    override val level: Level = Level.ERROR
    override val message: String
        get() = "Expected variant type, but got '$actual'"
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
        checkTypes(it, messages)
        checkThatAssignmentDoesNotChangeImmutableValue(it, messages)
    }

    return messages
}
