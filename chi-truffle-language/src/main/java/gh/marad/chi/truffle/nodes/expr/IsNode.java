package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.ChiObject;

import java.util.Objects;

@NodeChild(value = "value", type = ChiNode.class)
@NodeField(name = "variantName", type = String.class)
public abstract class IsNode extends ExpressionNode {
    protected abstract String getVariantName();

    @Specialization
    public boolean doChiObject(ChiObject object) {
        return getVariantName().equals(
                Objects.requireNonNull(object.getType().getVariant()).getVariantName());
    }
}
