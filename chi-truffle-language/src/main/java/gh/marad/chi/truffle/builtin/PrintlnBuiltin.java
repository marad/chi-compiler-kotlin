package gh.marad.chi.truffle.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.runtime.Unit;

public class PrintlnBuiltin extends Builtin {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var message = frame.getArguments()[0].toString();
        System.out.println(message);
        return Unit.instance;
    }


    @Override
    public Type type() {
        return Type.Companion.fn(Type.Companion.getUnit(), Type.Companion.getString());
    }

    @Override
    public String name() {
        return "println";
    }
}
