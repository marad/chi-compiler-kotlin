package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;

public class StringValue extends ValueNode {
    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public String executeString(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
