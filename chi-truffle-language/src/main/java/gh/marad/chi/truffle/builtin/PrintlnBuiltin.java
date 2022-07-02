package gh.marad.chi.truffle.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.runtime.Unit;

import java.io.OutputStream;
import java.io.PrintWriter;

public class PrintlnBuiltin extends Builtin {
    private final PrintWriter writer;

    public PrintlnBuiltin(OutputStream outputStream) {
        this.writer = new PrintWriter(outputStream);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        final Object message = ChiArgs.getArgument(frame, 0);
        printMessage(message);
        return Unit.instance;
    }

    @CompilerDirectives.TruffleBoundary
    private void printMessage(Object message) {
        writer.println(message);
        writer.flush();
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
