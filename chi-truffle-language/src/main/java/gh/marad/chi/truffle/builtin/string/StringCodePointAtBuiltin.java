package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringCodePointAtBuiltin extends Builtin {
    private final TruffleString.CodePointAtIndexNode node = TruffleString.CodePointAtIndexNode.create();

    @Override
    public FnType type() {
        return Type.fn(Type.getIntType(), Type.getString(), Type.getIntType());
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
        return "codePointAt";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        var index = (Long) ChiArgs.getArgument(frame, 1);
        return node.execute(string, index.intValue(), TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLong(frame);
    }
}
