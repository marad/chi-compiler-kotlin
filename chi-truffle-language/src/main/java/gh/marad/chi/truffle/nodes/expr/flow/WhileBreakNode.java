package gh.marad.chi.truffle.nodes.expr.flow;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class WhileBreakNode extends ExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw WhileBreakException.INSTANCE;
    }
}
