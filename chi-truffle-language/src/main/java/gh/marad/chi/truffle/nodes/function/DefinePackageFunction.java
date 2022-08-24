package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

public class DefinePackageFunction extends ExpressionNode {
    private final String moduleName;
    private final String packageName;
    private final ChiFunction function;
    private final Type[] paramTypes;

    public DefinePackageFunction(String moduleName, String packageName, ChiFunction function, Type[] paramTypes) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.function = function;
        this.paramTypes = paramTypes;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        var context = ChiContext.get(this);
        var module = context.modules.getOrCreateModule(moduleName);
        module.defineFunction(packageName, function, paramTypes);
        return function;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeFunction(frame);
    }
}
