package gh.marad.chi.actionast

import gh.marad.chi.core.FnType
import gh.marad.chi.core.Type
import gh.marad.chi.core.Program as CoreProgram
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
import gh.marad.chi.core.InfixOp as CoreInfixOp

sealed interface ActionAst {
    val type: Type

    companion object {
        fun from(exprs: List<CoreExpression>): List<ActionAst> {
            return exprs.map { from(it) }
        }

        fun from(it: CoreExpression): ActionAst {
            return when (it) {
                is CoreProgram -> Program(from(it.expressions))
                is CoreAtom -> Atom(it.value, it.type)
                is CoreNameDeclaration -> NameDeclaration(it.name, from(it.value), inferType(it.value))
                is CoreAssignment -> Assignment(it.name, from(it.value), inferType(it))
                is CoreVariableAccess -> VariableAccess(it.name, inferType(it))
                is CoreBlock -> Block(it.body.map { from(it) }, inferType(it))
                is CoreInfixOp -> InfixOp(it.op, from(it.left), from(it.right), inferType(it))
                is CoreFn -> {
                    Fn(
                        it.parameters.map { FnParam(it.name, it.type) },
                        it.returnType,
                        from(it.block) as Block
                    )
                }
                is CoreFnCall -> FnCall(it.name, it.parameters.map { from(it) }, inferType(it))
                is CoreIfElse -> IfElse(
                    condition = from(it.condition),
                    thenBranch = from(it.thenBranch) as Block,
                    elseBranch = it.elseBranch?.let { from(it) as Block },
                    type = inferType(it)
                )
            }
        }
    }
}

data class Program(val ast: List<ActionAst>) : ActionAst {
    override val type: Type = Type.unit
}

data class Atom(val value: String, override val type: Type) : ActionAst {
    companion object {
        val unit = Atom("()", Type.unit)
        val t = Atom("true", Type.bool)
        val f = Atom("false", Type.bool)
        fun i32(i: Int) = Atom("$i", Type.i32)
    }
}

data class NameDeclaration(val name: String, val value: ActionAst, override val type: Type) : ActionAst

data class Assignment(val name: String, val value: ActionAst, override val type: Type) : ActionAst

data class VariableAccess(val name: String, override val type: Type): ActionAst

data class Block(val body: List<ActionAst>, override val type: Type) : ActionAst

data class InfixOp(val op: String, val left: ActionAst, val right: ActionAst, override val type: Type) : ActionAst

data class FnParam(val name: String, val type: Type)
data class Fn(val parameters: List<FnParam>, val returnType: Type, val block: Block) : ActionAst {
    override val type = FnType(parameters.map { it.type }, returnType)
}

data class FnCall(val name: String, val parameters: List<ActionAst>, override val type: Type) : ActionAst

data class IfElse(val condition: ActionAst, val thenBranch: Block, val elseBranch: Block?, override val type: Type) : ActionAst
