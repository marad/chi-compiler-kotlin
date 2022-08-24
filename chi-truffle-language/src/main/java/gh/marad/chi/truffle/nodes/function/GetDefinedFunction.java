package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

public class GetDefinedFunction extends ExpressionNode {
    private final String moduleName;
    private final String packageName;
    private final String functionName;
    private final Type[] paramTypes;
    private ChiFunction cachedFn = null;
    private Assumption functionNotRedefined = Assumption.NEVER_VALID;

    public GetDefinedFunction(String moduleName, String packageName, String functionName, Type[] paramTypes) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.functionName = functionName;
        this.paramTypes = paramTypes;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        if (functionNotRedefined.isValid()) {
            return cachedFn;
        }

        var context = ChiContext.get(this);
        var module = context.modules.getOrCreateModule(moduleName);
        var lookupResult = module.findFunctionOrNull(packageName, functionName, paramTypes);
        if (lookupResult == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Function '%s' was not found in package %s/%s".formatted(functionName, moduleName, packageName));
        }
        cachedFn = lookupResult.function();
        functionNotRedefined = lookupResult.assumption();

        return cachedFn;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeFunction(frame);
    }
}
