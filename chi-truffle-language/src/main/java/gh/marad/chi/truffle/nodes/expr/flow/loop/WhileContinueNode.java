package gh.marad.chi.truffle.nodes.expr.flow.loop;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class WhileContinueNode extends ExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw WhileContinueException.INSTANCE;
    }
}
