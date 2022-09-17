package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;

public class WhileContinueNode extends ExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw WhileContinueException.INSTANCE;
    }
}
