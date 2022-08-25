package gh.marad.chi.truffle.runtime;

public class TODO extends RuntimeException {
    public TODO() {
        super("Not implemented yet!");
    }

    public TODO(String message) {
        super(message);
    }

    public TODO(Throwable cause) {
        super(cause);
    }

    public TODO(String message, Throwable cause) {
        super(message, cause);
    }
}
