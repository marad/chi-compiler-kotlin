package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringReplaceAllBuiltin extends Builtin {
    private final TruffleString.ToJavaStringNode toJava = TruffleString.ToJavaStringNode.create();
    private final TruffleString.FromJavaStringNode fromJava = TruffleString.FromJavaStringNode.create();


    @Override
    public Type type() {
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
        return "replaceAll";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        var toReplace = (TruffleString) ChiArgs.getArgument(frame, 1);
        var withWhat = (TruffleString) ChiArgs.getArgument(frame, 2);
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
        return haystack.replaceAll(needle, replacement);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
