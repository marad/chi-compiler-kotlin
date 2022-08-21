package gh.marad.chi.truffle.builtin.io;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

public class ArgsBuiltin extends Builtin {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var args = ChiContext.get(this).getEnv().getApplicationArguments();
        var truffleStrings = new TruffleString[args.length];
        for (var i = 0; i < args.length; i++) {
            truffleStrings[i] = TruffleString.fromJavaStringUncached(args[i], TruffleString.Encoding.UTF_8);
        }
        return new ChiArray(truffleStrings);
    }

    @Override
    public Type type() {
        return Type.fn(Type.array(Type.getString()));
    }

    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "io";
    }

    @Override
    public String name() {
        return "programArguments";
    }
}
