package gh.marad.chi.truffle;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import gh.marad.chi.core.*;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.nodes.expr.*;
import gh.marad.chi.truffle.nodes.expr.cast.CastToFloatNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToLongExprNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToStringNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.arithmetic.*;
import gh.marad.chi.truffle.nodes.expr.operators.bool.*;
import gh.marad.chi.truffle.nodes.expr.variables.*;
import gh.marad.chi.truffle.nodes.function.InvokeFunction;
import gh.marad.chi.truffle.nodes.value.*;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.TODO;

import java.util.ArrayList;
import java.util.List;

public class Converter {
    private final ChiLanguage language;
    private FrameDescriptor.Builder currentFdBuilder;

    public Converter(ChiLanguage language, FrameDescriptor.Builder fdBuilder) {
        this.language = language;
        this.currentFdBuilder = fdBuilder;
    }

    public ChiNode convertProgram(Program program) {
        var block = new BlockExpr(program.getExpressions().stream()
                                    .map(this::convertExpression)
                                    .toList());
        block.addRootTag();
        return block;
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
        else if (expr instanceof WhileLoop whileLoop) {
            return convertWhileExpr(whileLoop);
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
        var symbol = nameDeclaration.getEnclosingScope().getSymbol(nameDeclaration.getName());
        int slot = currentFdBuilder.addSlot(FrameSlotKind.Illegal, nameDeclaration.getName(), null);
        nameDeclaration.getEnclosingScope().updateSlot(nameDeclaration.getName(), slot);
        assert symbol != null : "Symbol not found for argument %s".formatted(nameDeclaration.getName());
         ChiNode valueExpr;
         if (nameDeclaration.getValue() instanceof Fn fn) {
             valueExpr = convertFnExprWithName(fn, nameDeclaration.getName());
         } else {
             valueExpr = convertExpression(nameDeclaration.getValue());
         }

        return WriteLocalVariableNodeGen.create(valueExpr, slot, nameDeclaration.getName());
    }

    private ChiNode convertVariableAccess(VariableAccess variableAccess) {
        var scope = variableAccess.getEnclosingScope();
        var symbolInfo = variableAccess.getEnclosingScope().getSymbol(variableAccess.getName());
        assert symbolInfo != null : "Symbol not found for local '%s'".formatted(variableAccess.getName());
        assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(variableAccess.getName());
        if (scope.isLocalSymbol(variableAccess.getName())) {
            return new ReadLocalVariable(variableAccess.getName(), symbolInfo.getSlot());
        } else {
            return new ReadOuterScope(variableAccess.getName());
        }
    }

    private ChiNode convertAssignment(Assignment assignment) {
        var scope = assignment.getEnclosingScope();
        var symbolInfo = scope.getSymbol(assignment.getName());
        assert symbolInfo != null : "Symbol not found for local '%s'".formatted(assignment.getName());
        assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(assignment.getName());
        if (scope.isLocalSymbol(assignment.getName())) {
            return WriteLocalVariableNodeGen.create(
                    convertExpression(assignment.getValue()),
                    symbolInfo.getSlot(),
                    assignment.getName());
        } else {
            return WriteOuterVariableNodeGen.create(
                    convertExpression(assignment.getValue()),
                    assignment.getName());
        }
    }

    private ChiNode convertBlock(Block block) {
        return convertBlock(block, null, null);
    }

    private ChiNode convertBlock(Block block, List<FnParam> fnParams, CompilationScope compilationScope) {

        var body = new ArrayList<ChiNode>();

        if (fnParams != null) {
            assert compilationScope != null : "Compilation scope cannot be null if fnParams is not null!";
            // this is function body block and we need to define arguments as local variables
            var argIndex = 1; // we start from 1 because 0 is always the parent scope frame
            for(var param : fnParams) {
                var symbol = compilationScope.getSymbol(param.getName());
                assert symbol != null : "Symbol not found for argument %s".formatted(param.getName());
                var localSlot = currentFdBuilder.addSlot(FrameSlotKind.Illegal, param.getName(), null);
                compilationScope.updateSlot(param.getName(), localSlot);
                //body.add(new ArgumentToLocalExpr(argIndex++, localSlot, param.getName()));
                body.add(WriteLocalVariableNodeGen.create(
                        new ReadLocalArgument(argIndex++),
                        localSlot,
                        param.getName()
                ));
            }
        }

        var actualBody = block.getBody().stream().map(this::convertExpression).toList();

        body.addAll(actualBody);

        var blockExpr = new BlockExpr(body);

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
        return convertFnExprWithName(fn, "[lambda]");
    }

    private ChiNode convertFnExprWithName(Fn fn, String name) {
        var previousFdBuilder = currentFdBuilder;
        currentFdBuilder = FrameDescriptor.newBuilder();
        var body = (ExpressionNode) convertBlock(fn.getBody(), fn.getParameters(), fn.getFnScope());
        body.addRootTag();
        var rootNode = new FnRootNode(language, currentFdBuilder.build(), body, name);
        currentFdBuilder = previousFdBuilder;

        var chiFunction = new ChiFunction(rootNode.getCallTarget());
        return new LambdaValue(chiFunction);
    }

    private ChiNode convertFnCall(FnCall fnCall) {
        var function = convertExpression(fnCall.getFunction());
        var parameters = fnCall.getParameters().stream().map(this::convertExpression).toList();
        return new InvokeFunction(function, parameters);
    }

    private ChiNode convertWhileExpr(WhileLoop whileLoop) {
        var condition = convertExpression(whileLoop.getCondition());
        var body = convertExpression(whileLoop.getLoop());
        return new WhileExprNode(condition, body);
    }
}
