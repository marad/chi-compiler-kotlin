package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;

public class TODO extends RuntimeException {
    @CompilerDirectives.TruffleBoundary
    public TODO() {
        super("Not implemented yet!");
        CompilerDirectives.transferToInterpreterAndInvalidate();
    }

    @CompilerDirectives.TruffleBoundary
    public TODO(String message) {
        super(message);
        CompilerDirectives.transferToInterpreterAndInvalidate();
    }

    @CompilerDirectives.TruffleBoundary
    public TODO(Throwable cause) {
        super(cause);
        CompilerDirectives.transferToInterpreterAndInvalidate();
    }

    @CompilerDirectives.TruffleBoundary
    public TODO(String message, Throwable cause) {
        super(message, cause);
        CompilerDirectives.transferToInterpreterAndInvalidate();
    }
}
