package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class BreakException extends ControlFlowException {
    public static final BreakException INSTANCE = new BreakException();

    private BreakException() {
    }
}
