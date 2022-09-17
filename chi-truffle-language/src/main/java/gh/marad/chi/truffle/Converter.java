package gh.marad.chi.truffle;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.RootNode;
import gh.marad.chi.core.Package;
import gh.marad.chi.core.*;
import gh.marad.chi.core.namespace.CompilationScope;
import gh.marad.chi.core.namespace.ScopeType;
import gh.marad.chi.core.namespace.SymbolType;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.nodes.IndexOperatorNodeGen;
import gh.marad.chi.truffle.nodes.IndexedAssignmentNodeGen;
import gh.marad.chi.truffle.nodes.expr.*;
import gh.marad.chi.truffle.nodes.expr.cast.CastToFloatNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToLongExprNodeGen;
import gh.marad.chi.truffle.nodes.expr.cast.CastToStringNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.arithmetic.*;
import gh.marad.chi.truffle.nodes.expr.operators.bit.BitAndOperatorNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.bit.BitOrOperatorNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.bit.ShlOperatorNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.bit.ShrOperatorNodeGen;
import gh.marad.chi.truffle.nodes.expr.operators.bool.*;
import gh.marad.chi.truffle.nodes.expr.variables.*;
import gh.marad.chi.truffle.nodes.function.DefinePackageFunction;
import gh.marad.chi.truffle.nodes.function.DefinePackageFunctionFromNodeGen;
import gh.marad.chi.truffle.nodes.function.GetDefinedFunction;
import gh.marad.chi.truffle.nodes.function.InvokeFunction;
import gh.marad.chi.truffle.nodes.objects.ConstructChiObject;
import gh.marad.chi.truffle.nodes.objects.ReadMemberNodeGen;
import gh.marad.chi.truffle.nodes.objects.WriteMemberNodeGen;
import gh.marad.chi.truffle.nodes.value.*;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.TODO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Converter {
    private final ChiLanguage language;
    private FrameDescriptor.Builder currentFdBuilder;

    private String currentModule = CompilationDefaults.INSTANCE.getDefaultModule();
    private String currentPackage = CompilationDefaults.INSTANCE.getDefaultPacakge();

    public Converter(ChiLanguage language, FrameDescriptor.Builder fdBuilder) {
        this.language = language;
        this.currentFdBuilder = fdBuilder;
    }

    public ChiNode convertProgram(Program program) {
        var body = program.getExpressions().stream()
                          .map(this::convertExpression)
                          .filter(Objects::nonNull)
                          .toList()
                          .toArray(new ChiNode[0]);
        if (body.length > 0) {
            var block = new BlockExpr(body);
            block.addRootTag();
            return block;
        } else {
            return new UnitValue();
        }
    }

    public ChiNode convertExpression(Expression expr) {
        if (expr instanceof Atom atom) {
            return convertAtom(atom);
        } else if (expr instanceof NameDeclaration nameDeclaration) {
            return convertNameDeclaration(nameDeclaration);
        } else if (expr instanceof VariableAccess variableAccess) {
            return convertVariableAccess(variableAccess);
        } else if (expr instanceof FieldAccess fieldAccess) {
            return convertFieldAccess(fieldAccess);
        } else if (expr instanceof FieldAssignment assignment) {
            return convertFieldAssignment(assignment);
        } else if (expr instanceof Block block) {
            return convertBlock(block);
        } else if (expr instanceof InfixOp infixOp) {
            return convertInfixOp(infixOp);
        } else if (expr instanceof PrefixOp prefixOp) {
            return convertPrefixOp(prefixOp);
        } else if (expr instanceof Cast cast) {
            return convertCast(cast);
        } else if (expr instanceof IfElse ifElse) {
            return convertIfExpr(ifElse);
        } else if (expr instanceof Fn fn) {
            return convertFnExpr(fn);
        } else if (expr instanceof FnCall fnCall) {
            return convertFnCall(fnCall);
        } else if (expr instanceof Group group) {
            return convertExpression(group.getValue());
        } else if (expr instanceof Assignment assignment) {
            return convertAssignment(assignment);
        } else if (expr instanceof WhileLoop whileLoop) {
            return convertWhileExpr(whileLoop);
        } else if (expr instanceof Break) {
            return new BreakNode();
        } else if (expr instanceof Package pkg) {
            currentModule = pkg.getModuleName();
            currentPackage = pkg.getPackageName();
            return null; // skip this node
        } else if (expr instanceof Import) {
            return null; // skip this node
        } else if (expr instanceof DefineVariantType definition) {
            return convertAndCreateCompositeTypeConstructors(definition);
        } else if (expr instanceof IndexOperator op) {
            return IndexOperatorNodeGen.create(
                    convertExpression(op.getVariable()),
                    convertExpression(op.getIndex())
            );
        } else if (expr instanceof IndexedAssignment op) {
            return IndexedAssignmentNodeGen.create(
                    convertExpression(op.getVariable()),
                    convertExpression(op.getIndex()),
                    convertExpression(op.getValue())
            );
        } else if (expr instanceof Is is) {
            return convertIs(is);
        }

        throw new TODO("Unhandled expression conversion: %s".formatted(expr));
    }

    private ChiNode convertAndCreateCompositeTypeConstructors(DefineVariantType expr) {
        return convertGenericCompositeTypesToDynamicObjects(expr);
    }

    private ChiNode convertGenericCompositeTypesToDynamicObjects(DefineVariantType expr) {
        var constructorDefinitions =
                expr.getConstructors().stream()
                    .map(constructor -> {
                        var constructorFunction = createFunctionFromNode(
                                new ConstructChiObject(
                                        language,
                                        expr.getBaseVariantType().withVariant(constructor.toVariant())),
                                constructor.getName());
                        if (constructor.getFields().isEmpty()) {
                            return WriteModuleVariableNodeGen.create(
                                    new InvokeFunction(new LambdaValue(constructorFunction), Collections.emptyList()),
                                    currentModule,
                                    currentPackage,
                                    constructor.getName()
                            );
                        } else {
                            var paramTypes = constructor.getFields().stream().map(VariantTypeField::getType).toList().toArray(new Type[0]);
                            return new DefinePackageFunction(
                                    currentModule,
                                    currentPackage,
                                    new ChiFunction(constructorFunction),
                                    paramTypes);
                        }
                    }).collect(Collectors.toList());
        constructorDefinitions.add(new UnitValue());
        return new BlockExpr(constructorDefinitions.toArray(new ChiNode[0]));
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
        var scope = nameDeclaration.getEnclosingScope();
        var symbol = scope.getSymbol(nameDeclaration.getName(), true);
        assert symbol != null : "Symbol not found for name %s".formatted(nameDeclaration.getName());

        if (symbol.getScopeType() == ScopeType.Package && nameDeclaration.getValue() instanceof Fn fn) {
            return convertModuleFunctionDefinitionFromFunctionNode(
                    nameDeclaration.getName(),
                    convertFnExpr(fn, nameDeclaration.getName()),
                    (FnType) fn.getType()
            );
        } else if (symbol.getScopeType() == ScopeType.Package && nameDeclaration.getValue().getType() instanceof FnType fnType) {
            return convertModuleFunctionDefinitionFromFunctionNode(
                    nameDeclaration.getName(),
                    convertExpression(nameDeclaration.getValue()),
                    fnType
            );
        } else if (symbol.getScopeType() == ScopeType.Package) {
            return WriteModuleVariableNodeGen.create(
                    convertExpression(nameDeclaration.getValue()),
                    currentModule, currentPackage, nameDeclaration.getName());
        } else {
            int slot = currentFdBuilder.addSlot(FrameSlotKind.Illegal, nameDeclaration.getName(), null);
            scope.updateSlot(nameDeclaration.getName(), slot);
            ChiNode valueExpr = convertExpression(nameDeclaration.getValue());
            return WriteLocalVariableNodeGen.create(valueExpr, slot, nameDeclaration.getName());
        }
    }

    private ChiNode convertVariableAccess(VariableAccess variableAccess) {
        var scope = variableAccess.getDefinitionScope();
        var symbolInfo = scope.getSymbol(variableAccess.getName(), true);
        assert symbolInfo != null : "Symbol not found for local '%s'".formatted(variableAccess.getName());
        if (symbolInfo.getScopeType() == ScopeType.Package) {
            return new ReadModuleVariable(
                    variableAccess.getModuleName(),
                    variableAccess.getPackageName(),
                    variableAccess.getName()
            );
        } else if (symbolInfo.getSymbolType() == SymbolType.Local && scope.containsInNonVirtualScope(variableAccess.getName())) {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(variableAccess.getName());
            return new ReadLocalVariable(variableAccess.getName(), symbolInfo.getSlot());
        } else if (symbolInfo.getSymbolType() == SymbolType.Local) {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(variableAccess.getName());
            return new ReadOuterScopeVariable(variableAccess.getName());
        } else if (symbolInfo.getSymbolType() == SymbolType.Argument && scope.containsInNonVirtualScope(variableAccess.getName())) {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(variableAccess.getName());
            return new ReadLocalArgument(symbolInfo.getSlot());
        } else {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(variableAccess.getName());
            var scopesUp = scope.countNonVirtualScopesToName(variableAccess.getName());
            return new ReadOuterScopeArgument(scopesUp, symbolInfo.getSlot());
        }
    }

    private ChiNode convertFieldAccess(FieldAccess fieldAccess) {
        return ReadMemberNodeGen.create(convertExpression(fieldAccess.getReceiver()), fieldAccess.getFieldName());
    }

    private ChiNode convertFieldAssignment(FieldAssignment assignment) {
        var receiver = convertExpression(assignment.getReceiver());
        var value = convertExpression(assignment.getValue());
        return WriteMemberNodeGen.create(receiver, value, assignment.getFieldName());
    }

    private ChiNode convertAssignment(Assignment assignment) {
        var scope = assignment.getDefinitionScope();
        var symbolInfo = scope.getSymbol(assignment.getName(), true);
        assert symbolInfo != null : "Symbol not found for local '%s'".formatted(assignment.getName());
        if (symbolInfo.getScopeType() == ScopeType.Package) {
            return WriteModuleVariableNodeGen.create(
                    convertExpression(assignment.getValue()),
                    currentModule,
                    currentPackage,
                    assignment.getName()
            );
        } else if (symbolInfo.getSymbolType() == SymbolType.Local) {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(assignment.getName());
            if (scope.containsInNonVirtualScope(assignment.getName())) {
                return WriteLocalVariableNodeGen.create(
                        convertExpression(assignment.getValue()),
                        symbolInfo.getSlot(),
                        assignment.getName());
            } else {
                return WriteOuterVariableNodeGen.create(
                        convertExpression(assignment.getValue()),
                        assignment.getName()
                );
            }
        } else if (symbolInfo.getSymbolType() == SymbolType.Argument) {
            assert symbolInfo.getSlot() != -1 : "Slot for local '%s' was not set up!".formatted(assignment.getName());
            return WriteLocalArgumentNodeGen.create(
                    convertExpression(assignment.getValue()),
                    symbolInfo.getSlot()
            );
        }
        throw new TODO("This should not happen");
    }

    private ChiNode convertBlock(Block block) {
        return convertBlock(block, null, null, null);
    }

    private ChiNode convertBlock(Block block, Type returnType, List<FnParam> fnParams, CompilationScope compilationScope) {
        if (fnParams != null) {
            assert compilationScope != null : "Compilation scope cannot be null if fnParams is not null!";
            var argIndex = 0;
            for (var param : fnParams) {
                var symbol = compilationScope.getSymbol(param.getName(), true);
                assert symbol != null : "Symbol not found for argument %s".formatted(param.getName());
                assert symbol.getSymbolType() == SymbolType.Argument : String.format("Symbol '%s' is not an argument", param.getName());
                compilationScope.updateSlot(param.getName(), argIndex);
                argIndex += 1;
            }
        }

        var body = new ArrayList<>(block.getBody().stream().map(this::convertExpression).toList());
        if (returnType == Type.getUnit()) {
            body.add(new UnitValue());
        }
        return new BlockExpr(body.toArray(new ChiNode[0]));
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
            case "&" -> BitAndOperatorNodeGen.create(left, right);
            case "|" -> BitOrOperatorNodeGen.create(left, right);
            case "<<" -> ShlOperatorNodeGen.create(left, right);
            case ">>" -> ShrOperatorNodeGen.create(left, right);
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
        if (cast.getTargetType() == Type.getIntType()) {
            return CastToLongExprNodeGen.create(value);
        } else if (cast.getTargetType() == Type.getFloatType()) {
            return CastToFloatNodeGen.create(value);
        } else if (cast.getTargetType() == Type.getString()) {
            return CastToStringNodeGen.create(value);
        } else if (cast.getTargetType().isCompositeType()) {
            return value;
        } else {
            return value;
        }
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

    private ChiNode convertFnExpr(Fn fn, String name) {
        var functionCallTarget = createFunctionWithName(fn, name);
        return new LambdaValue(functionCallTarget);
    }

    private ChiNode convertFnExpr(Fn fn) {
        return convertFnExpr(fn, "[lambda]");
    }

    private ChiNode convertModuleFunctionDefinitionFromFunctionNode(String name, ChiNode fnExprNode, FnType type) {
        var paramTypes = type.getParamTypes().toArray(new Type[0]);
        return DefinePackageFunctionFromNodeGen.create(fnExprNode, currentModule, currentPackage, name, paramTypes);
    }

    private RootCallTarget createFunctionFromNode(ExpressionNode body, String name) {
        RootNode rootNode = withNewFrameDescriptor(
                () -> new FnRootNode(language, currentFdBuilder.build(), body, name));
        return rootNode.getCallTarget();
    }

    private RootCallTarget createFunctionWithName(Fn fn, String name) {
        var rootNode = withNewFrameDescriptor(() -> {
            var body = (ExpressionNode) convertBlock(fn.getBody(), fn.getReturnType(), fn.getParameters(), fn.getFnScope());
            body.addRootTag();
            return new FnRootNode(language, currentFdBuilder.build(), body, name);
        });
        return rootNode.getCallTarget();
    }

    private <T> T withNewFrameDescriptor(Supplier<T> f) {
        var previousFdBuilder = currentFdBuilder;
        currentFdBuilder = FrameDescriptor.newBuilder();
        var result = f.get();
        currentFdBuilder = previousFdBuilder;
        return result;
    }

    private ChiNode convertFnCall(FnCall fnCall) {
        var functionExpr = fnCall.getFunction();
        FnType fnType;
        if (functionExpr.getType() instanceof OverloadedFnType overloaded) {
            fnType = overloaded.getType(fnCall.getParameters().stream().map(Expression::getType).toList());
        } else if (functionExpr.getType() instanceof FnType type) {
            fnType = type;
        } else {
            throw new TODO("This is not a function type %s".formatted(functionExpr.getType()));
        }
        assert fnType != null;
        var paramTypes = fnType.getParamTypes().toArray(new Type[0]);
        var parameters = fnCall.getParameters().stream().map(this::convertExpression).toList();
        if (functionExpr instanceof VariableAccess variableAccess) {
            var scope = variableAccess.getDefinitionScope();
            var symbol = scope.getSymbol(variableAccess.getName(), true);
            assert symbol != null : "Symbol not found for name %s".formatted(variableAccess.getName());
            var symbolType = symbol.getSymbolType();
            if (symbol.getScopeType() == ScopeType.Package) {
                var function = new GetDefinedFunction(
                        variableAccess.getModuleName(),
                        variableAccess.getPackageName(),
                        variableAccess.getName(),
                        paramTypes);
                return new InvokeFunction(function, parameters);
            } else if (symbolType == SymbolType.Local || symbolType == SymbolType.Argument) {
                var function = convertExpression(functionExpr);
                return new InvokeFunction(function, parameters);
            } else {
                throw new TODO("Dedicated error here. You should not be here!");
            }
        } else {
            var function = convertExpression(functionExpr);
            return new InvokeFunction(function, parameters);
        }
    }

    private ChiNode convertWhileExpr(WhileLoop whileLoop) {
        var condition = convertExpression(whileLoop.getCondition());
        var body = convertExpression(whileLoop.getLoop());
        return new WhileExprNode(condition, body);
    }

    private ChiNode convertIs(Is is) {
        return IsNodeGen.create(convertExpression(is.getValue()), is.getTypeOrVariant());
    }
}
