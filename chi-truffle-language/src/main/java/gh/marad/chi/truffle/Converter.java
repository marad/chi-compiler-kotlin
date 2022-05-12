package gh.marad.chi.truffle;

import com.oracle.truffle.api.frame.FrameDescriptor;
import gh.marad.chi.core.*;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.ChiRootNode;
import gh.marad.chi.truffle.nodes.expr.*;
import gh.marad.chi.truffle.nodes.expr.cast.CastToFloatNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToLongExprNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToStringNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.arithmetic.*;
import gh.marad.chi.truffle.nodes.expr.operators.bool.*;
import gh.marad.chi.truffle.nodes.function.InvokeFunction;
import gh.marad.chi.truffle.nodes.value.*;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.TODO;

public class Converter {
    private final ChiLanguage language;
    private LexicalScope currentScope;

    public Converter(ChiLanguage language, LexicalScope enclosingScope) {
        this.language = language;
        this.currentScope = enclosingScope;
    }

    public ChiNode convertProgram(Program program) {
        return new BlockExpr(program.getExpressions().stream()
                                    .map(this::convertExpression)
                                    .toList());
    }

    public ChiNode convertExpression(Expression expr) {
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
        else if (expr instanceof PrefixOp prefixOp) {
            return convertPrefixOp(prefixOp);
        }
        else if (expr instanceof Cast cast) {
            return convertCast(cast);
        }
        else if (expr instanceof IfElse ifElse) {
            return convertIfExpr(ifElse);
        }
        else if (expr instanceof Fn fn) {
            return convertFnExpr(fn);
        }
        else if (expr instanceof FnCall fnCall) {
            return convertFnCall(fnCall);
        }
        else if (expr instanceof Group group) {
            return convertExpression(group.getValue());
        }
        else if (expr instanceof Assignment assignment) {
            return convertAssignment(assignment);
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
        var symbolInfo = variableAccess.getEnclosingScope().getSymbol(variableAccess.getName());
        if (symbolInfo.getScope() == SymbolScope.Local) {
            return new ReadVariableExpr(variableAccess.getName(), currentScope);
        } else {
            return new ReadArgumentExpr(symbolInfo.getSlot());
        }
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
        return switch (infixOp.getOp()) {
            case "+" -> PlusOperatorNodeGen.create(left, right);
            case "-" -> MinusOperatorNodeGen.create(left, right);
            case "*" -> MultiplyOperatorNodeGen.create(left, right);
            case "/" -> DivideOperatorNodeGen.create(left, right);
            case "%" -> ModuloOperatorNodeGen.create(left, right);
            case "==" -> EqualOperatorNodeGen.create(left, right);
            case "!=" -> NotEqualOperatorNodeGen.create(left, right);
            case "<" -> LessThanOperatorNodeGen.create(false, left, right);
            case "<=" -> LessThanOperatorNodeGen.create(true, left, right);
            case ">" -> GreaterThanOperatorNodeGen.create(false, left, right);
            case ">=" -> GreaterThanOperatorNodeGen.create(true, left, right);
            case "&&" -> new LogicAndOperator(left, right);
            case "||" -> new LogicOrOperator(left, right);
            default -> throw new TODO("Unhandled infix operator: '%s'".formatted(infixOp.getOp()));
        };
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private ChiNode convertPrefixOp(PrefixOp prefixOp) {
        var value = convertExpression(prefixOp.getExpr());
        return switch (prefixOp.getOp()) {
            case "!" -> LogicNotOperatorNodeGen.create(value);
            default -> throw new TODO("Unhandled prefix operator: '%s'".formatted(prefixOp.getOp()));
        };
    }

    private ChiNode convertCast(Cast cast) {
        var value = convertExpression(cast.getExpression());
        if (cast.getTargetType() == Type.Companion.getIntType()) {
            return CastToLongExprNodeGen.create(value);
        }
        else if (cast.getTargetType() == Type.Companion.getFloatType()) {
            return CastToFloatNodeGen.create(value);
        }
        else if (cast.getTargetType() == Type.Companion.getString()) {
            return CastToStringNodeGen.create(value);
        }
        throw new TODO("Unhandled cast from '%s' to '%s'".formatted(
                cast.getType().getName(), cast.getTargetType().getName()));
    }

    private ChiNode convertIfExpr(IfElse ifElse) {
        var condition = convertExpression(ifElse.getCondition());
        var thenBranch = convertExpression(ifElse.getThenBranch());
        ChiNode elseBranch;
        if (ifElse.getElseBranch() != null) {
            elseBranch = convertExpression(ifElse.getElseBranch());
        } else {
            elseBranch = null;
        }
        return IfExpr.create(condition, thenBranch, elseBranch);
    }

    private ChiNode convertFnExpr(Fn fn) {
        var body = convertExpression(fn.getBody());
        var rootNode = new ChiRootNode(language, FrameDescriptor.newBuilder().build(), body);
        var chiFunction = new ChiFunction(rootNode.getCallTarget());
        return new LambdaValue(chiFunction);
    }

    private ChiNode convertFnCall(FnCall fnCall) {
        var function = convertExpression(fnCall.getFunction());
        var parameters = fnCall.getParameters().stream().map(this::convertExpression).toList();
        return new InvokeFunction(function, parameters);
    }

    private ChiNode convertAssignment(Assignment assignment) {
        return new AssignmentExpr(
                assignment.getName(),
                convertExpression(assignment.getValue()),
                currentScope
        );
    }
}
