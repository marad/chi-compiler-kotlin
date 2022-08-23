package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringLengthBuiltin extends Builtin {
    private final TruffleString.CodePointLengthNode node = TruffleString.CodePointLengthNode.create();

    @Override
    public FnType type() {
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
        return "length";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        return node.execute((TruffleString) ChiArgs.getArgument(frame, 0), TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLong(frame);
    }
}
