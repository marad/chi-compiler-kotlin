package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class HandleEffectNode extends ExpressionNode {

    @Child
    private ResumableBlockNode resumableBlockNode;

    public HandleEffectNode(ResumableBlockNode resumableBlockNode) {
        this.resumableBlockNode = resumableBlockNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return resumableBlockNode.executeGeneric(frame);
        } catch (InvokeEffectException ex) {
            System.out.println(ex.getEffectName());
            return ex.resume("hello");
        }
    }
}
