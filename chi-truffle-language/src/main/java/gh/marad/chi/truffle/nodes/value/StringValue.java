package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;

public class StringValue extends ValueNode {
    private final TruffleString value;

    public StringValue(TruffleString value) {
        this.value = value;
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
