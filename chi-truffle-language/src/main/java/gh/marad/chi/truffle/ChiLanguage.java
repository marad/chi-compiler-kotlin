package gh.marad.chi.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import gh.marad.chi.core.Compiler;
import gh.marad.chi.core.VariantType;
import gh.marad.chi.core.analyzer.Level;
import gh.marad.chi.truffle.compilation.CompilationFailed;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.ChiObject;

@TruffleLanguage.Registration(
        id = ChiLanguage.id,
        name = "Chi",
        defaultMimeType = ChiLanguage.mimeType,
        characterMimeTypes = ChiLanguage.mimeType,
        contextPolicy = TruffleLanguage.ContextPolicy.SHARED
)
@ProvidedTags({StandardTags.RootTag.class, StandardTags.ExpressionTag.class, StandardTags.RootBodyTag.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class})
public class ChiLanguage extends TruffleLanguage<ChiContext> {
    public static final String id = "chi";
    public static final String mimeType = "application/x-chi";
    private static final LanguageReference<ChiLanguage> REFERENCE = LanguageReference.create(ChiLanguage.class);

    private final Shape initialObjectShape = Shape.newBuilder().build();

    public Object createObject(String[] fieldNames, VariantType variant, TruffleLanguage.Env env) {
        return new ChiObject(fieldNames, variant, initialObjectShape, env);
    }

    public static ChiLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    @Override
    protected ChiContext createContext(Env env) {
        return new ChiContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        var source = request.getSource();
        var sourceString = source.getCharacters().toString();
        return compile(sourceString);
    }

    @CompilerDirectives.TruffleBoundary
    public CallTarget compile(String sourceString) {
        var context = ChiContext.get(null);
        var compiled = Compiler.compile(sourceString, context.compilationNamespace);

        if (compiled.hasErrors()) {
            compiled.getMessages().forEach(message -> {
                var msgStr = Compiler.formatCompilationMessage(sourceString, message);
                if (message.getLevel() == Level.ERROR) {
                    System.err.println(msgStr);
                } else {
                    System.out.println(msgStr);
                }
            });
            CompilerDirectives.transferToInterpreter();
            throw new CompilationFailed(compiled.getMessages());
        }

        var fdBuilder = FrameDescriptor.newBuilder();
        var converter = new Converter(this, fdBuilder);
        var executableAst = converter.convertProgram(compiled.getProgram());
        var rootNode = new ProgramRootNode(this, executableAst, fdBuilder.build());
        return rootNode.getCallTarget();
    }

    private void printAst(ChiNode node) {
        printAst(node, "");
    }

    private void printAst(Node node, String prefix) {
        System.out.printf("%s%s%n", prefix, node.getClass().getName());
        node.getChildren().forEach(child -> printAst(child, prefix + "  "));
    }
}
