package gh.marad.chi.truffle.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;

public class MillisBuiltin extends Builtin {
    @Override
    public Type type() {
        return Type.Companion.fn(Type.Companion.getIntType());
    }

    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "time";
    }

    @Override
    public String name() {
        return "millis";
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        return System.currentTimeMillis();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return System.currentTimeMillis();
    }
}
