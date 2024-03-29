package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

public class ToLowerBuiltin extends Builtin {
    @Child
    private TruffleString.ToJavaStringNode toJava = TruffleString.ToJavaStringNode.create();
    @Child
    private TruffleString.FromJavaStringNode fromJava = TruffleString.FromJavaStringNode.create();


    @Override
    public FnType type() {
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
        return "toLower";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var string = ChiArgs.getTruffleString(frame, 0);
        var javaString = toJava.execute(string);
        return fromJava.execute(toLower(javaString), TruffleString.Encoding.UTF_8);
    }

    @CompilerDirectives.TruffleBoundary
    private String toLower(String s) {
        return s.toLowerCase();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
