package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class IndexOfCodePointBuiltin extends Builtin {
    @Child
    private TruffleString.IndexOfCodePointNode node = TruffleString.IndexOfCodePointNode.create();

    @Override
    public FnType type() {
        return Type.fn(Type.getIntType(), Type.getString(), Type.getIntType(), Type.getIntType(), Type.getIntType());
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
        return "indexOfCodePoint";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        var string = ChiArgs.getTruffleString(frame, 0);
        var codePoint = ChiArgs.getLong(frame, 1);
        var start = ChiArgs.getLong(frame, 2);
        var end = ChiArgs.getLong(frame, 3);
        return node.execute(string, (int) codePoint, (int) start, (int) end, TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLong(frame);
    }
}
