package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;

public class DoubleValue extends ValueNode {
    private final double value;

    public DoubleValue(double value) {
        this.value = value;
    }

    @Override
    public double executeDouble(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
