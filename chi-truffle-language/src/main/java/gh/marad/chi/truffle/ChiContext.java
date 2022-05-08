package gh.marad.chi.truffle;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;

public class ChiContext {
    private static final TruffleLanguage.ContextReference<ChiContext> REFERENCE = TruffleLanguage.ContextReference.create(ChiLanguage.class);
    public static ChiContext get(Node node) { return REFERENCE.get(node); }
    private final ChiLanguage chiLanguage;
    private final TruffleLanguage.Env env;

    public ChiContext(ChiLanguage chiLanguage, TruffleLanguage.Env env) {
        this.chiLanguage = chiLanguage;
        this.env = env;
    }
}
