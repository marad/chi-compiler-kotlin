package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ReadLocalArgument extends ExpressionNode {
    private final int slot;

    public ReadLocalArgument(int slot) {
        this.slot = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getArguments()[slot];
    }
}
