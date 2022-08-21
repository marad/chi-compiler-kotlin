package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class ToUpperBuiltin extends Builtin {
    private final TruffleString.ToJavaStringNode toJava = TruffleString.ToJavaStringNode.create();
    private final TruffleString.FromJavaStringNode fromJava = TruffleString.FromJavaStringNode.create();


    @Override
    public Type type() {
        return Type.fn(Type.getString(), Type.getString());
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
        return "toUpper";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        var javaString = toJava.execute(string);
        return fromJava.execute(toUpper(javaString), TruffleString.Encoding.UTF_8);
    }

    @CompilerDirectives.TruffleBoundary
    private String toUpper(String s) {
        return s.toUpperCase();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
