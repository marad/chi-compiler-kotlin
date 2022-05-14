package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class AssignmentExpr extends ChiNode {
    private final String name;
    private final ChiNode valueNode;
    private final LexicalScope scope;
    private final int slot;

    public AssignmentExpr(String name, ChiNode valueNode, LexicalScope scope, int slot) {
        this.name = name;
        this.valueNode = valueNode;
        this.scope = scope;
        this.slot = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var value = valueNode.executeGeneric(frame);
//        scope.setValue(name, value);
//        frame.setObject(slot, value);
        setVariableValue(name, value);
        return value;
    }

    public void setVariableValue(String name, Object value) {
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            var frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            var desc = frame.getFrameDescriptor();
            for (var i = 0; i < desc.getNumberOfSlots(); i++) {
                if (name.equals(desc.getSlotName(i))) {
                    frame.setObject(i, value);
                    return true;
                }
            }
            return null;
        });
    }
}
