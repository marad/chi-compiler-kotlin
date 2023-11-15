package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.builtin.Builtin;

public class StringFromCodePointsBuiltin extends Builtin {
    @Child
    private TruffleString.FromJavaStringNode node = TruffleString.FromJavaStringNode.create();

    @Override
    public FnType type() {
        return Type.fn(Type.getString(), Type.array(Type.getIntType()));
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
        return "fromCodePoints";
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var codePointArray = ChiArgs.getChiArray(frame, 0);
        var objects = codePointArray.unsafeGetUnderlayingArray();
        var codePoints = new int[objects.length];
        for (int i = 0; i < objects.length; i++) {
            codePoints[i] = (int) ChiTypesGen.asImplicitLong(objects[i]);
        }
        var s = makeString(codePoints);
        return node.execute(s, TruffleString.Encoding.UTF_8);
    }

    @CompilerDirectives.TruffleBoundary
    private String makeString(int[] codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
