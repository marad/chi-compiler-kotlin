package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public abstract class ValueNode extends ExpressionNode {
    @Override
    public void executeVoid(VirtualFrame frame) {
        // no need to do anything if the value is not going to be used
    }
}
