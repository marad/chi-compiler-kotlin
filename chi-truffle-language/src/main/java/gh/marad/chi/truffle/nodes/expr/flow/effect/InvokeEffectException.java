package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;

import java.util.function.Function;

public class InvokeEffectException extends ControlFlowException {
    private final String effectName;
    private final VirtualFrame effectFrame;
    private final int resumeValueSlot;
    private final Object[] arguments;

    private Function<Object, Object> resumeExecution;

    public InvokeEffectException(String effectName, VirtualFrame effectFrame, int resumeValueSlot, Object[] arguments) {
        this.effectName = effectName;
        this.effectFrame = effectFrame;
        this.resumeValueSlot = resumeValueSlot;
        this.arguments = arguments;
    }

    public String getEffectName() {
        return effectName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void registerBlock(VirtualFrame frame, ResumableBlockNode blockNode) {
        var currentResume = resumeExecution;
        resumeExecution = (value) -> blockNode.resumeEffectWith(frame, value);
    }

    public Object resume(Object value) {
        return resumeExecution.apply(value);
    }
}
