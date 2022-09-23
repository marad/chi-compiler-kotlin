package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class ResumeValueException extends ControlFlowException {
    private final Object value;

    public ResumeValueException(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
