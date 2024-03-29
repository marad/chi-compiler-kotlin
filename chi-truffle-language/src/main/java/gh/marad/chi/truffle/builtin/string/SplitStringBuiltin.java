package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

public class SplitStringBuiltin extends Builtin {
    @Child
    private TruffleString.ToJavaStringNode toJava = TruffleString.ToJavaStringNode.create();
    @Child
    private TruffleString.FromJavaStringNode fromJava = TruffleString.FromJavaStringNode.create();


    @Override
    public FnType type() {
        return Type.fn(Type.array(Type.getString()), Type.getString(), Type.getString(), Type.getIntType());
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
        return "split";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var string = ChiArgs.getTruffleString(frame, 0);
        var splitter = ChiArgs.getTruffleString(frame, 1);
        var limit = ChiArgs.getLong(frame, 2);
        return split(toJava.execute(string), toJava.execute(splitter), (int) limit);
    }

    @CompilerDirectives.TruffleBoundary
    private ChiArray split(String s, String splitter, int limit) {
        var result = s.split(splitter, limit);
        var data = new TruffleString[result.length];
        for (int i = 0; i < result.length; i++) {
            data[i] = fromJava.execute(result[i], TruffleString.Encoding.UTF_8);
        }
        return new ChiArray(data);
    }
}
