package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;

public class TODO extends RuntimeException {
    @CompilerDirectives.TruffleBoundary
    public TODO() {
        super("Not implemented yet!");
    }

    @CompilerDirectives.TruffleBoundary
    public TODO(String message) {
        super(message);
    }
}
