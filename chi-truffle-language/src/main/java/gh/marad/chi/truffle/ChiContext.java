package gh.marad.chi.truffle;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import gh.marad.chi.core.CompilationScope;
import gh.marad.chi.core.SymbolScope;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.builtin.MillisBuiltin;
import gh.marad.chi.truffle.builtin.PrintlnBuiltin;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class ChiContext {
    private static final TruffleLanguage.ContextReference<ChiContext> REFERENCE = TruffleLanguage.ContextReference.create(ChiLanguage.class);
    public static ChiContext get(Node node) { return REFERENCE.get(node); }

    public final LexicalScope globalScope;
    public final CompilationScope globalCompilationScope;

    private final ChiLanguage chiLanguage;
    private final TruffleLanguage.Env env;

    public ChiContext(ChiLanguage chiLanguage, TruffleLanguage.Env env) {
        this.chiLanguage = chiLanguage;
        this.env = env;
        this.globalScope = new LexicalScope();
        this.globalCompilationScope = new CompilationScope();
        installBuiltins();
    }

    private void installBuiltins() {
        installBuiltin(new PrintlnBuiltin());
        installBuiltin(new MillisBuiltin());
    }

    private void installBuiltin(Builtin node) {
        var rootNode = new FnRootNode(chiLanguage, FrameDescriptor.newBuilder().build(), node);
        var fn = new ChiFunction(rootNode.getCallTarget());
        globalScope.defineValue(node.name(), fn);
        globalCompilationScope.addSymbol(node.name(), node.type(), SymbolScope.Local);
    }
}
