package gh.marad.chi.truffle.nodes.expr.flow;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.core.VariantType;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiObject;

import java.util.Objects;

@NodeChild(value = "value", type = ChiNode.class)
@NodeField(name = "typeName", type = String.class)
public abstract class IsNode extends ExpressionNode {
    protected abstract String getTypeName();

    @Specialization
    public boolean doChiObject(ChiObject object) {
        var type = object.getType();
        return variantNameMatches(type) || typeNameMatches(type);
    }

    private boolean variantNameMatches(VariantType type) {
        return getTypeName().equals(
                Objects.requireNonNull(type.getVariant()).getVariantName());
    }

    private boolean typeNameMatches(VariantType type) {
        return getTypeName().equals(type.getSimpleName());
    }
}
