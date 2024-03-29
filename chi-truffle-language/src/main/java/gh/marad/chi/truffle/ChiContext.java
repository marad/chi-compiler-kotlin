package gh.marad.chi.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import gh.marad.chi.core.Type;
import gh.marad.chi.core.namespace.GlobalCompilationNamespace;
import gh.marad.chi.core.namespace.SymbolType;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.builtin.Prelude;
import gh.marad.chi.truffle.builtin.collections.ArrayBuiltin;
import gh.marad.chi.truffle.builtin.collections.SizeBuiltin;
import gh.marad.chi.truffle.builtin.io.*;
import gh.marad.chi.truffle.builtin.lang.EvalBuiltin;
import gh.marad.chi.truffle.builtin.lang.interop.LookupHostSymbolBuiltin;
import gh.marad.chi.truffle.builtin.lang.interop.array.HasArrayElementsBuiltin;
import gh.marad.chi.truffle.builtin.lang.interop.members.*;
import gh.marad.chi.truffle.builtin.lang.interop.values.IsNullBuiltin;
import gh.marad.chi.truffle.builtin.lang.usafe.UnsafeArrayBuiltin;
import gh.marad.chi.truffle.builtin.string.*;
import gh.marad.chi.truffle.builtin.time.MillisBuiltin;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.namespaces.Modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ChiContext {
    private static final TruffleLanguage.ContextReference<ChiContext> REFERENCE = TruffleLanguage.ContextReference.create(ChiLanguage.class);

    public static ChiContext get(Node node) {
        return REFERENCE.get(node);
    }

    public final LexicalScope globalScope;
    public final GlobalCompilationNamespace compilationNamespace;
    private EffectHandlers currentEffectHandlers;

    private final ChiLanguage chiLanguage;
    private final TruffleLanguage.Env env;

    public final Modules modules = new Modules();


    public ChiContext(ChiLanguage chiLanguage, TruffleLanguage.Env env) {
        this.chiLanguage = chiLanguage;
        this.env = env;
        this.compilationNamespace = new GlobalCompilationNamespace(Prelude.imports);
        this.currentEffectHandlers = new EffectHandlers(null, new HashMap<>());

        List<Builtin> builtins = List.of(
                // lang
                new EvalBuiltin(chiLanguage),
                // lang.unsafe
                new UnsafeArrayBuiltin(),
                // lang.interop
                new LookupHostSymbolBuiltin(env),
                new HasMembersBuiltin(),
                new GetMembersBuiltin(),
                new IsMemberReadableBuiltin(),
                new IsMemberModifiable(),
                new IsMemberInsertable(),
                new IsMemberRemovableBuiltin(),
                new IsMemberInvocableBuiltin(),
                new IsMemberInternalBuiltin(),
                new IsMemberWritableBuiltin(),
                new IsMemberExistingBuiltin(),
                new HasMemberReadSideEffectsBuiltin(),
                new HasMemberWriteSideEffectsBuiltin(),
                new ReadMemberBuiltin(),
                new WriteMemberBuiltin(),
                new RemoveMemberBuiltin(),
                new InvokeMemberBuiltin(),
                new IsNullBuiltin(),
                // io
                new PrintBuiltin(env.out()),
                new PrintlnBuiltin(env.out()),
                new ReadLinesBuiltin(),
                new ReadStringBuiltin(),
                new ArgsBuiltin(),
                // time
                new MillisBuiltin(),
                // collections
                new ArrayBuiltin(),
                new SizeBuiltin(),
                new HasArrayElementsBuiltin(),
                // string
                new StringLengthBuiltin(),
                new StringCodePointAtBuiltin(),
                new SubstringBuiltin(),
                new StringHashBuiltin(),
                new StringCodePointsBuiltin(),
                new StringFromCodePointsBuiltin(),
                new IndexOfCodePointBuiltin(),
                new IndexOfStringBuiltin(),
                new ToUpperBuiltin(),
                new ToLowerBuiltin(),
                new SplitStringBuiltin(),
                new StringReplaceBuiltin(),
                new StringReplaceAllBuiltin()
        );
        var frameDescriptor = FrameDescriptor.newBuilder().build();
        this.globalScope = new LexicalScope(Truffle.getRuntime().createMaterializedFrame(new Object[0], frameDescriptor));
        installBuiltins(builtins);
    }

    private void installBuiltins(List<Builtin> builtins) {
        builtins.forEach(this::installBuiltin);
    }

    private void installBuiltin(Builtin node) {
        var rootNode = new FnRootNode(chiLanguage, FrameDescriptor.newBuilder().build(), node, node.name());
        var fn = new ChiFunction(rootNode.getCallTarget());
        modules.getOrCreateModule(node.getModuleName())
               .defineFunction(node.getPackageName(), fn, node.type().getParamTypes().toArray(new Type[0]));
        var compilationScope = compilationNamespace.getOrCreatePackage(
                node.getModuleName(),
                node.getPackageName()
        ).getScope();
        compilationScope.addSymbol(node.name(), node.type(), SymbolType.Local, true, false);
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public <T> T withEffectHandlers(Map<EffectHandlers.Qualifier, ChiFunction> handlers, Supplier<T> code) {
        var previousHandler = currentEffectHandlers;
        currentEffectHandlers = new EffectHandlers(previousHandler, handlers);
        var result = code.get();
        currentEffectHandlers = previousHandler;
        return result;
    }

    public ChiFunction findEffectHandlerOrNull(EffectHandlers.Qualifier qualifier) {
        return currentEffectHandlers.findEffectHandlerOrNull(qualifier);
    }
}
