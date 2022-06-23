package gh.marad.chi.truffle.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.runtime.Unit;

import java.io.PrintWriter;

public class PrintlnBuiltin extends Builtin {
    private PrintWriter writer;

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var message = ChiArgs.getArgument(frame, 0).toString();
        var context = ChiContext.get(this);
        writer = new PrintWriter(context.getEnv().out());
        writer.println(message);
        writer.flush();
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
