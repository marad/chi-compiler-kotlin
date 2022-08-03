package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BlockNode;
import gh.marad.chi.truffle.nodes.ChiNode;

public class BlockExpr extends ExpressionNode implements BlockNode.ElementExecutor<ChiNode> {
    @Child
    private BlockNode<ChiNode> block;

    public BlockExpr(ChiNode[] elements) {
        this.block = BlockNode.create(elements, this);
    }

    @Override
    public void executeVoid(VirtualFrame frame, ChiNode node, int index, int argument) {
        node.executeVoid(frame);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, ChiNode node, int index, int argument) {
        return node.executeGeneric(frame);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return block.executeGeneric(frame, 0);
    }
}
