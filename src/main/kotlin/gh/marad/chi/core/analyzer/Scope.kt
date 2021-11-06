package gh.marad.chi.core.analyzer

import gh.marad.chi.actionast.ActionAst
import gh.marad.chi.core.Expression
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.Type

class Scope<T>(private val parentScope: Scope<T>? = null) {
    private val variables = mutableMapOf<String, T>()
    private val externalNames = mutableMapOf<String, Type>()

    fun defineVariable(name: String, value: T) {
        variables[name] = value
    }

    fun defineExternalName(name: String, type: Type) {
        externalNames[name] = type
    }

    fun getExternalNameType(externalName: String): Type? =
        externalNames[externalName]
            ?: parentScope?.getExternalNameType(externalName)

    fun findVariable(name: String): T? =
        variables[name]
            ?: parentScope?.findVariable(name)

    companion object {
        fun fromExpressions(expression: List<Expression>, parentScope: Scope<Expression>? = null): Scope<Expression> {
            val scope = Scope(parentScope)
            expression.forEach { expr ->
                if (expr is NameDeclaration) {
                    scope.defineVariable(expr.name, expr.value)
                }
            }
            return scope
        }

        fun fromActionAst(asts: List<ActionAst>, parentScope: Scope<ActionAst>? = null): Scope<ActionAst> {
            val scope = Scope(parentScope)
            asts.forEach { ast ->
                if (ast is gh.marad.chi.actionast.NameDeclaration) {
                    scope.defineVariable(ast.name, ast.value)
                }
            }
            return scope
        }
    }
}

