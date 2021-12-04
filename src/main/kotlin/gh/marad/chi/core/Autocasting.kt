package gh.marad.chi.core

import gh.marad.chi.core.analyzer.isSubType

fun Type.canCastTo(targetType: Type): Boolean {
    return isSubType(this, targetType)
}

fun Type.canDowncastTo(targetType: Type): Boolean =
    this.isNumber() && targetType.isNumber()

fun Expression.castTo(targetType: Type): Cast = Cast(this, targetType, location)

fun automaticallyCastCompatibleTypes(expression: Expression): Expression {
    return mapAst(expression) { exp ->
        when (exp) {
            is InfixOp -> {
                val leftType = exp.left.type
                val rightType = exp.right.type
                if (leftType.canCastTo(rightType)) {
                    exp.copy(left = exp.left.castTo(rightType))
                } else if (rightType.canCastTo(leftType)) {
                    exp.copy(right = exp.right.castTo(leftType))
                } else {
                    exp
                }
            }
//            is NameDeclaration -> {
//                val exprType = exp.value.type
//                if (exp.expectedType != null && exp.expectedType != exprType &&
//                    (exprType.canCastTo(exp.expectedType) || exprType.canDowncastTo(exp.expectedType))) {
//                    exp.copy(value = exp.value.castTo(exp.expectedType))
//                } else {
//                    exp
//                }
//            }
//            is Assignment -> {
//                val scope = exp.enclosingScope
//                val expectedType = scope.getLocalName("")
//                val exprType = inferType(exp.value)
//                if (exp.expectedType != null && exp.expectedType != exprType && exprType.canCastTo(exp.expectedType)) {
//                    exp.copy(value = exp.value.castTo(exp.expectedType))
//                } else {
//                    exp
//                }
//            }
            else -> { exp }
        }
    }
}
