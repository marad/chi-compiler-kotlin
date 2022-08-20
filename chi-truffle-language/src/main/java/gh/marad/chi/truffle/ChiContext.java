package gh.marad.chi.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;
import gh.marad.chi.core.GlobalCompilationNamespace;
import gh.marad.chi.core.SymbolScope;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.builtin.Prelude;
import gh.marad.chi.truffle.builtin.collections.ArrayBuiltin;
import gh.marad.chi.truffle.builtin.io.ArgsBuiltin;
import gh.marad.chi.truffle.builtin.io.PrintBuiltin;
import gh.marad.chi.truffle.builtin.io.PrintlnBuiltin;
import gh.marad.chi.truffle.builtin.io.ReadLinesBuiltin;
import gh.marad.chi.truffle.builtin.string.StringCodePointAtBuiltin;
import gh.marad.chi.truffle.builtin.string.StringLengthBuiltin;
import gh.marad.chi.truffle.builtin.time.MillisBuiltin;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.namespaces.Modules;

import java.util.List;

public class ChiContext {
    private static final TruffleLanguage.ContextReference<ChiContext> REFERENCE = TruffleLanguage.ContextReference.create(ChiLanguage.class);

    public static ChiContext get(Node node) {
        return REFERENCE.get(node);
    }

    public final LexicalScope globalScope;
    public final GlobalCompilationNamespace compilationNamespace;

    private final ChiLanguage chiLanguage;
    private final TruffleLanguage.Env env;

    public final Modules modules = new Modules();

    public ChiContext(ChiLanguage chiLanguage, TruffleLanguage.Env env) {
        this.chiLanguage = chiLanguage;
        this.env = env;
        this.compilationNamespace = new GlobalCompilationNamespace(Prelude.imports);

        List<Builtin> builtins = List.of(
                // io
                new PrintBuiltin(env.out()),
                new PrintlnBuiltin(env.out()),
                new ReadLinesBuiltin(),
                new ArgsBuiltin(),
                // time
                new MillisBuiltin(),
                // collections
                new ArrayBuiltin(),
                // string
                new StringLengthBuiltin(),
                new StringCodePointAtBuiltin()
        );
        var frameDescriptor = prepareFrameDescriptor(builtins);
        this.globalScope = new LexicalScope(Truffle.getRuntime().createMaterializedFrame(new Object[0], frameDescriptor));
        installBuiltins(builtins);
    }

    private FrameDescriptor prepareFrameDescriptor(List<Builtin> builtins) {
        var fdBuilder = FrameDescriptor.newBuilder();
        builtins.forEach(builtin -> fdBuilder.addSlot(FrameSlotKind.Object, builtin.name(), null));
        return fdBuilder.build();
    }

    private void installBuiltins(List<Builtin> builtins) {
        builtins.forEach(this::installBuiltin);
    }

    private void installBuiltin(Builtin node) {
        var rootNode = new FnRootNode(chiLanguage, FrameDescriptor.newBuilder().build(), node, node.name());
        var fn = new ChiFunction(rootNode.getCallTarget());
        modules.getOrCreateModule(node.getModuleName())
               .defineFunction(node.getPackageName(), fn);
        var compilationScope = compilationNamespace.getOrCreatePackage(
                node.getModuleName(),
                node.getPackageName()
        ).getScope();
        compilationScope.addSymbol(node.name(), node.type(), SymbolScope.Package, false);
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }
}
