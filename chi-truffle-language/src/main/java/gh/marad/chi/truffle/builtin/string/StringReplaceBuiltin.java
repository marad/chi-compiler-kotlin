package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringReplaceBuiltin extends Builtin {
    @Child
    private TruffleString.ToJavaStringNode toJava = TruffleString.ToJavaStringNode.create();
    @Child
    private TruffleString.FromJavaStringNode fromJava = TruffleString.FromJavaStringNode.create();


    @Override
    public FnType type() {
        return Type.fn(Type.getString(), Type.getString(), Type.getString(), Type.getString());
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
        return "replace";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var string = ChiArgs.getTruffleString(frame, 0);
        var toReplace = ChiArgs.getTruffleString(frame, 1);
        var withWhat = ChiArgs.getTruffleString(frame, 2);
        return fromJava.execute(
                replaceAll(
                        toJava.execute(string),
                        toJava.execute(toReplace),
                        toJava.execute(withWhat)
                ),
                TruffleString.Encoding.UTF_8);
    }

    @CompilerDirectives.TruffleBoundary
    public String replaceAll(String haystack, String needle, String replacement) {
        return haystack.replace(needle, replacement);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
