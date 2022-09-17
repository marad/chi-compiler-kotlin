package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

@NodeChild(value = "function", type = ChiNode.class)
@NodeField(name = "moduleName", type = String.class)
@NodeField(name = "packageName", type = String.class)
@NodeField(name = "functionName", type = String.class)
@NodeField(name = "paramTypes", type = Type[].class)
public abstract class DefinePackageFunctionFromNode extends ExpressionNode {

    protected abstract String getModuleName();

    protected abstract String getPackageName();

    protected abstract String getFunctionName();

    protected abstract Type[] getParamTypes();

    @Specialization
    public ChiFunction defineModuleFunction(ChiFunction function) {
        var context = ChiContext.get(this);
        var module = context.modules.getOrCreateModule(getModuleName());
        module.defineNamedFunction(getPackageName(), getFunctionName(), function, getParamTypes());
        return function;

    }
}
