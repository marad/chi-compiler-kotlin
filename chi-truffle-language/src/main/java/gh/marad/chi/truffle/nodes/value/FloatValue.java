package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;

public class FloatValue extends ValueNode {
    private final float value;

    public FloatValue(float value) {
        this.value = value;
    }

    @Override
    public float executeFloat(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
