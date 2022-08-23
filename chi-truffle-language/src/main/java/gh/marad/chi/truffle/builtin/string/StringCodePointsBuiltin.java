package gh.marad.chi.truffle.builtin.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

public class StringCodePointsBuiltin extends Builtin {
    private final TruffleString.CodePointLengthNode codePointLength = TruffleString.CodePointLengthNode.create();
    private final TruffleString.CreateCodePointIteratorNode node = TruffleString.CreateCodePointIteratorNode.create();

    @Override
    public FnType type() {
        return Type.fn(Type.array(Type.getIntType()), Type.getString());
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
        return "codePoints";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var string = (TruffleString) ChiArgs.getArgument(frame, 0);
        var length = codePointLength.execute(string, TruffleString.Encoding.UTF_8);
        var iterator = node.execute(string, TruffleString.Encoding.UTF_8);
        var data = new Long[length];
        var index = 0;
        while (iterator.hasNext()) {
            data[index++] = (long) iterator.nextUncached();
        }
        return new ChiArray(data);
    }
}
