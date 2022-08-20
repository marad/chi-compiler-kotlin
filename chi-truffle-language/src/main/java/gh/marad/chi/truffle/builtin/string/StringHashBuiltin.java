package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringHashBuiltin extends Builtin {
    private final TruffleString.HashCodeNode node = TruffleString.HashCodeNode.create();

    @Override
    public Type type() {
        return Type.fn(Type.getIntType(), Type.getString());
    }

    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "string";
    }

    @Override
    public String name() {
        return "hash";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        return node.execute(string, TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLong(frame);
    }
}
