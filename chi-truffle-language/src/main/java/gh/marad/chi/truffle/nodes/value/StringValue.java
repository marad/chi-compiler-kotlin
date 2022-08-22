package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;

import java.nio.charset.StandardCharsets;

public class StringValue extends ValueNode {
    private final TruffleString value;

    public StringValue(String value) {
        this.value = TruffleString.fromByteArrayUncached(value.getBytes(StandardCharsets.UTF_8), TruffleString.Encoding.UTF_8);
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
