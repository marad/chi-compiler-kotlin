package gh.marad.chi.core

import gh.marad.chi.core.Type.Companion.i32

/**
 * Simplifies AST so it's easier to evaluate and/or emit C code.
 *
 * Things that it does:
 * - takes inline functions outside, renames them to avoid collisions and updates call sites accordingly
 */
fun simplify(asts: List<Expression>): List<Expression> {
    return asts
}

internal fun makeIfAnExpression(tmpVarName: String, ifElse: IfElse): List<Expression> {
    fun Block.replaceLastExpressionWithAssignment(name: String): Block {
        return this
    }

    return listOf(
        NameDeclaration(tmpVarName, Atom("0", i32, ifElse.location), false, i32, ifElse.location),
        IfElse(
            condition = ifElse.condition,
            thenBranch = ifElse.thenBranch.replaceLastExpressionWithAssignment(tmpVarName),
            elseBranch = ifElse.elseBranch?.replaceLastExpressionWithAssignment(tmpVarName),
            location = ifElse.location
        ),
        VariableAccess(tmpVarName, ifElse.location)
    )
}