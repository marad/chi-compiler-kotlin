package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class IndexOfStringBuiltin extends Builtin {
    @Child
    private TruffleString.IndexOfStringNode node = TruffleString.IndexOfStringNode.create();

    @Override
    public FnType type() {
        return Type.fn(Type.getIntType(), Type.getString(), Type.getString(), Type.getIntType(), Type.getIntType());
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
        return "indexOf";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        var haystack = ChiArgs.getTruffleString(frame, 0);
        var needle = ChiArgs.getTruffleString(frame, 1);
        var start = ChiArgs.getLong(frame, 2);
        var end = ChiArgs.getLong(frame, 3);
        return node.execute(haystack, needle, (int) start, (int) end, TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLong(frame);
    }
}
