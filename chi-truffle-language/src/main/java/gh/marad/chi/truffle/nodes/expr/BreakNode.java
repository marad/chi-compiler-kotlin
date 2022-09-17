package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;

public class BreakNode extends ExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw BreakException.INSTANCE;
    }
}
