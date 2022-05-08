package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;

public class IntValue extends ValueNode {
    private final int value;

    public IntValue(int value) {
        this.value = value;
    }

    @Override
    public long executeInt(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
