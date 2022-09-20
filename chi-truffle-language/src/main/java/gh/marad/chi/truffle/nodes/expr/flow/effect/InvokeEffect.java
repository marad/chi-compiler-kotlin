package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class InvokeEffect extends ExpressionNode {
    private final String effectName;
    private final int resumeValueSlot;

    public InvokeEffect(String effectName, int resumeValueSlot) {
        this.effectName = effectName;
        this.resumeValueSlot = resumeValueSlot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var resumeValue = frame.getObject(resumeValueSlot);
        if (resumeValue == null) {
            throw new InvokeEffectException(effectName, frame, resumeValueSlot, frame.getArguments());
        } else {
            return resumeValue;
        }
    }
}
