package gh.marad.chi.truffle;

import gh.marad.chi.core.*;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.BlockExpr;
import gh.marad.chi.truffle.nodes.expr.DeclareNameExpr;
import gh.marad.chi.truffle.nodes.expr.ReadVariableExpr;
import gh.marad.chi.truffle.nodes.expr.operators.*;
import gh.marad.chi.truffle.nodes.value.*;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.TODO;

public class Converter {
    private LexicalScope currentScope;

    public Converter(LexicalScope enclosingScope) {
        this.currentScope = enclosingScope;
    }

    public ChiNode convertProgram(Program program) {
        return new BlockExpr(program.getExpressions().stream()
                                    .map(this::convertExpression)
                                    .toList());
    }

    private ChiNode convertExpression(Expression expr) {
        if (expr instanceof Atom atom)  {
            return convertAtom(atom);
        }
        else if (expr instanceof NameDeclaration nameDeclaration) {
            return convertNameDeclaration(nameDeclaration);
        }
        else if (expr instanceof VariableAccess variableAccess) {
            return convertVariableAccess(variableAccess);
        }
        else if (expr instanceof Block block) {
            return convertBlock(block);
        }
        else if (expr instanceof InfixOp infixOp) {
            return convertInfixOp(infixOp);
        }
        throw new TODO("Unhandled expression conversion: %s".formatted(expr));
    }

    private ChiNode convertAtom(Atom atom) {
        if (atom.getType() == Type.Companion.getIntType()) {
            return new LongValue(Long.parseLong(atom.getValue()));
        }
        if (atom.getType() == Type.Companion.getFloatType()) {
            return new FloatValue(Float.parseFloat(atom.getValue()));
        }
        if (atom.getType() == Type.Companion.getString()) {
            return new StringValue(atom.getValue());
        }
        if (atom.getType() == Type.Companion.getBool()) {
            return new BooleanValue(Boolean.parseBoolean(atom.getValue()));
        }
        throw new TODO("Unhandled atom type: %s".formatted(atom.getType()));
    }

    private ChiNode convertNameDeclaration(NameDeclaration nameDeclaration) {
        return new DeclareNameExpr(nameDeclaration.getName(), currentScope, convertExpression(nameDeclaration.getValue()));
    }

    private ChiNode convertVariableAccess(VariableAccess variableAccess) {
        return new ReadVariableExpr(variableAccess.getName(), currentScope);
    }

    private ChiNode convertBlock(Block block) {
        var parentScope = currentScope;
        currentScope = new LexicalScope(parentScope);

        var body = block.getBody().stream().map(this::convertExpression).toList();
        var blockExpr = new BlockExpr(body);

        currentScope = parentScope;
        return blockExpr;
    }

    private ChiNode convertInfixOp(InfixOp infixOp) {
        var left = convertExpression(infixOp.getLeft());
        var right = convertExpression(infixOp.getRight());
        switch (infixOp.getOp()) {
            case "+":
                return PlusOperatorNodeGen.create(left, right);
            case "-":
                return MinusOperatorNodeGen.create(left, right);
            case "*":
                return MultiplyOperatorNodeGen.create(left, right);
            case "/":
                return DivideOperatorNodeGen.create(left, right);
            case "%":
                return ModuloOperatorNodeGen.create(left, right);
            default:
                throw new TODO("Unhandled infix operator!");
        }
    }
}
