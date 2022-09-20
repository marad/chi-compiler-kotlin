package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BlockNode;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ResumableBlockNode extends ExpressionNode implements BlockNode.ElementExecutor<ChiNode> {
    @CompilerDirectives.CompilationFinal
    private Integer indexSlot;
    private Object resumeValueKey = new Object();
    private final int resumeValueSlot;
    @Child
    private BlockNode<ChiNode> block;

    public ResumableBlockNode(int resumeValueSlot, ChiNode[] body) {
        this.resumeValueSlot = resumeValueSlot;
        this.block = BlockNode.create(body, this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        frame.setAuxiliarySlot(getIndexSlot(frame), 0);
        return block.executeGeneric(frame, 0);
    }

    public Object resume(VirtualFrame frame) {
        getIndexSlot(frame);
        int startIndex = (int) frame.getAuxiliarySlot(getIndexSlot(frame));
        return block.executeGeneric(frame, startIndex);
    }

    public Object resumeEffectWith(VirtualFrame frame, Object value) {
        getIndexSlot(frame);
        int startIndex = (int) frame.getAuxiliarySlot(getIndexSlot(frame));
        return block.executeGeneric(frame, startIndex);
    }

    @Override
    public void executeVoid(VirtualFrame frame, ChiNode node, int index, int argument) {
        executeGeneric(frame, node, index, argument);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, ChiNode node, int elementIndex, int startIndex) {
        if (elementIndex >= startIndex) {
            try {
                return node.executeGeneric(frame);
            } catch (InvokeEffectException e) {
                frame.setAuxiliarySlot(getIndexSlot(frame), elementIndex);
                e.registerBlock(frame, this);
                throw e;
            }
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new AssertionError("Invalid start index!");
        }
    }

    private int getIndexSlot(VirtualFrame frame) {
        var slot = this.indexSlot;
        if (slot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameDescriptor fd = frame.getFrameDescriptor();
            this.indexSlot = slot = fd.findOrAddAuxiliarySlot(this);
        }
        return slot;
    }
}
