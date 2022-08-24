package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

@NodeField(name = "moduleName", type = String.class)
@NodeField(name = "packageName", type = String.class)
@NodeField(name = "variableName", type = String.class)
@NodeChild(value = "value", type = ChiNode.class)
public abstract class WriteModuleVariable extends ExpressionNode {
    protected abstract String getModuleName();

    protected abstract String getPackageName();

    protected abstract String getVariableName();

    @Specialization
    public Object saveObject(Object value) {
        var ctx = ChiContext.get(this);
        ctx.modules.getOrCreateModule(getModuleName())
                   .defineVariable(getPackageName(), getVariableName(), value);
        return value;
    }
}
