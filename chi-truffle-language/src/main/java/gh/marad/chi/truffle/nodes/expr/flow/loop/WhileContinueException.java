package gh.marad.chi.truffle.nodes.expr.flow.loop;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class WhileContinueException extends ControlFlowException {
    public static final WhileContinueException INSTANCE = new WhileContinueException();

    private WhileContinueException() {
    }
}
