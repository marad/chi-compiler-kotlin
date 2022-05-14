package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.Unit;

public class ArgumentToLocalExpr extends ChiNode  {
    private final int argumentIndex;
    private final int localSlot;

    public ArgumentToLocalExpr(int argumentIndex, int localSlot) {
        this.argumentIndex = argumentIndex;
        this.localSlot = localSlot;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        var arguments = frame.getArguments();
        frame.setObject(localSlot, arguments[argumentIndex]);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        executeVoid(frame);
        return Unit.instance;
    }
}
