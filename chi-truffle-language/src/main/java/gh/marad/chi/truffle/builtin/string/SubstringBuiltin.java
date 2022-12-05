package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class SubstringBuiltin extends Builtin {
    @Child
    private TruffleString.SubstringNode node = TruffleString.SubstringNode.create();

    @Override
    public FnType type() {
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
        var string = ChiArgs.getTruffleString(frame, 0);
        var start = ChiArgs.getLong(frame, 1);
        var length = ChiArgs.getLong(frame, 2);
        return node.execute(string, (int) start, (int) length, TruffleString.Encoding.UTF_8, false);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
