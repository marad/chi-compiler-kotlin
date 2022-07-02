package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;

public class NoSuchPackageException extends RuntimeException {
    @CompilerDirectives.TruffleBoundary
    public NoSuchPackageException(String packageName) {
        super("Package %s does not exist".formatted(packageName));
    }
}
