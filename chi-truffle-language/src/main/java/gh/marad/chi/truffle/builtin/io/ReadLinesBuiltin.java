package gh.marad.chi.truffle.builtin.io;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadLinesBuiltin extends Builtin {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var filePath = ChiArgs.getTruffleString(frame, 0);
        return readLines(filePath);
    }

    @CompilerDirectives.TruffleBoundary
    private ChiArray readLines(TruffleString path) {
        try {
            var lines = Files.readAllLines(Path.of(path.toJavaStringUncached()));
            var data = lines.stream()
                            .map(it -> TruffleString.fromJavaStringUncached(it, TruffleString.Encoding.UTF_8))
                            .toList();
            return new ChiArray(data.toArray(new TruffleString[0]));
        } catch (IOException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }

    @Override
    public FnType type() {
        return Type.fn(Type.array(Type.getString()), Type.getString());
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
        return "readLines";
    }
}
