package gh.marad.chi.actionast

import gh.marad.chi.core.Atom
import gh.marad.chi.core.Block
import gh.marad.chi.core.Expression
import gh.marad.chi.core.IfElse
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type.Companion.i32
import gh.marad.chi.core.VariableAccess
import gh.marad.chi.core.analyzer.Scope

/**
 * Simplifies AST so it's easier to evaluate and/or emit C code.
 *
 * Things that it does:
 * - takes inline functions outside, renames them to avoid collisions and updates call sites accordingly
 * - changes if expressions to functions to make them expressions
 */
fun simplify(asts: List<Expression>): List<ActionAst> {
    return ActionAst.from(Scope() ,asts)
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