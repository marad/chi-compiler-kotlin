package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ReadModuleVariable extends ExpressionNode {
    private final String moduleName;
    private final String packageName;
    private final String variableName;

    public ReadModuleVariable(String moduleName, String packageName, String variableName) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.variableName = variableName;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var ctx = ChiContext.get(this);
        return ctx.modules.getOrCreateModule(moduleName)
                          .findVariableFunctionOrNull(packageName, variableName);

    }
}
