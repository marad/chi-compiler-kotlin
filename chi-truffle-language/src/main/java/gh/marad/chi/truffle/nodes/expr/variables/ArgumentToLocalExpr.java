package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.Unit;

public class ArgumentToLocalExpr extends ExpressionNode {
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

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.WriteVariableTag.class || super.hasTag(tag);
    }
}
