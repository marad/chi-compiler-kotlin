package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

public class StringFromCodePointsBuiltin extends Builtin {
    private final TruffleString.FromJavaStringNode node = TruffleString.FromJavaStringNode.create();

    @Override
    public Type type() {
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
        var codePointArray = (ChiArray) ChiArgs.getArgument(frame, 0);
        var objects = codePointArray.unsafeGetUnderlayingArray();
        var codePoints = new int[objects.length];
        for (int i = 0; i < objects.length; i++) {
            codePoints[i] = ((Long) objects[i]).intValue();
        }
        var s = new String(codePoints, 0, codePoints.length);
        return node.execute(s, TruffleString.Encoding.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }
}
