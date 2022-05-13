package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;

public class ReadArgumentExpr extends ChiNode {
    private final String name;
    private final int argumentIndex;

    public ReadArgumentExpr(String name, int slot) {
        this.name = name;
        this.argumentIndex = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return frame.getArguments()[argumentIndex];
        } catch (Throwable ex) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Argument %s not found at slot %d!".formatted(name, argumentIndex));
        }
    }
}
