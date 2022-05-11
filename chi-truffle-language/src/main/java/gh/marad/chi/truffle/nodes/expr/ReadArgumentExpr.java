package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;

public class ReadArgumentExpr extends ChiNode {
    private final int argumentIndex;

    public ReadArgumentExpr(int slot) {
        this.argumentIndex = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getArguments()[argumentIndex];
    }
}
