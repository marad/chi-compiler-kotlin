package gh.marad.chi.actionast

import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.analyzer.Scope
import gh.marad.chi.core.analyzer.inferType
import gh.marad.chi.core.Assignment as CoreAssignment
import gh.marad.chi.core.Atom as CoreAtom
import gh.marad.chi.core.Expression as CoreExpression
import gh.marad.chi.core.Fn as CoreFn
import gh.marad.chi.core.FnCall as CoreFnCall
import gh.marad.chi.core.IfElse as CoreIfElse
import gh.marad.chi.core.NameDeclaration as CoreNameDeclaration
import gh.marad.chi.core.VariableAccess as CoreVariableAccess
import gh.marad.chi.core.Block as CoreBlock

sealed interface ActionAst {
    companion object {
        fun from(parentScope: Scope<CoreExpression>, exprs: List<CoreExpression>): List<ActionAst> {
            val scope = Scope.fromExpressions(exprs, parentScope)
            return exprs.map { from(scope, it) }
        }

        fun from(scope: Scope<CoreExpression>, it: CoreExpression): ActionAst {
            return when (it) {
                is CoreAtom -> Atom(it.value, it.type)
                is CoreNameDeclaration -> NameDeclaration(it.name, from(scope, it.value), inferType(scope, it.value))
                is CoreAssignment -> Assignment(it.name, from(scope, it.value))
                is CoreVariableAccess -> VariableAccess(it.name)
                is CoreBlock -> Block(it.body.map { from(scope, it) })
                is CoreFn -> {
                    val subscope = Scope.fromExpressions(it.block.body, scope)
                    Fn(
                        it.parameters.map { FnParam(it.name, it.type) },
                        it.returnType,
                        from(subscope, it.block) as Block
                    )
                }
                is CoreFnCall -> FnCall(it.name, it.parameters.map { from(scope, it) })
                is CoreIfElse -> IfElse(
                    condition = from(scope, it.condition),
                    thenBranch = from(scope, it.thenBranch) as Block,
                    elseBranch = it.elseBranch?.let { from(scope, it) } as Block
                )
            }
        }
    }
}

data class Atom(val value: String, val type: Type) : ActionAst

data class NameDeclaration(val name: String, val value: ActionAst, val type: Type) : ActionAst

data class Assignment(val name: String, val value: ActionAst) : ActionAst

data class VariableAccess( val name: String): ActionAst

data class Block(val body: List<ActionAst>) : ActionAst

data class FnParam(val name: String, val type: Type)
data class Fn(val parameters: List<FnParam>, val returnType: Type, val block: Block) : ActionAst {
    val type = FnType(parameters.map { it.type }, returnType)
}

data class FnCall(val name: String, val parameters: List<ActionAst>) : ActionAst

data class IfElse(val condition: ActionAst, val thenBranch: Block, val elseBranch: Block?) : ActionAst
