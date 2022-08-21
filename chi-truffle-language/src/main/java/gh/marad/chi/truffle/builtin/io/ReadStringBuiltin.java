package gh.marad.chi.truffle.builtin.io;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadStringBuiltin extends Builtin {

    private final TruffleString.FromJavaStringNode node = TruffleString.FromJavaStringNode.create();

    @Override
    public TruffleString executeString(VirtualFrame frame) {
        var filePath = (TruffleString) ChiArgs.getArgument(frame, 0);
        return readString(filePath);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeString(frame);
    }

    @CompilerDirectives.TruffleBoundary
    private TruffleString readString(TruffleString path) {
        try {
            var javaString = Files.readString(Path.of(path.toJavaStringUncached()));
            return node.execute(javaString, TruffleString.Encoding.UTF_8);
        } catch (IOException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Type type() {
        return Type.fn(Type.getString(), Type.getString());
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
        return "readString";
    }
}
