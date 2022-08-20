package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class SubstringBuiltin extends Builtin {
    private final TruffleString.SubstringNode lengthNode = TruffleString.SubstringNode.create();

    @Override
    public Type type() {
        return Type.fn(Type.getString(), Type.getString(), Type.getIntType(), Type.getIntType());
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
        return "substring";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        var start = (Long) ChiArgs.getArgument(frame, 1);
        var end = (Long) ChiArgs.getArgument(frame, 2);
        return lengthNode.execute(string, start.intValue(), end.intValue(), TruffleString.Encoding.UTF_8, false);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
