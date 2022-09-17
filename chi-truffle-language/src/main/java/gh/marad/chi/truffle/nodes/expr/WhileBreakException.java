package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class WhileBreakException extends ControlFlowException {
    public static final WhileBreakException INSTANCE = new WhileBreakException();

    private WhileBreakException() {
    }
}
