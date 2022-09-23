package gh.marad.chi.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.truffle.runtime.ChiFunction;

import java.util.Map;

public class EffectHandlers {
    private final EffectHandlers parent;
    private final Map<Qualifier, ChiFunction> handlers;

    public record Qualifier(String module, String pkg, String name) {
    }

    public EffectHandlers(EffectHandlers parent, Map<Qualifier, ChiFunction> handlers) {
        this.parent = parent;
        this.handlers = handlers;
    }

    @CompilerDirectives.TruffleBoundary
    public ChiFunction findEffectHandlerOrNull(Qualifier qualifier) {
        var result = handlers.get(qualifier);
        if (result == null && parent != null) {
            return parent.findEffectHandlerOrNull(qualifier);
        }
        return result;
    }
}
