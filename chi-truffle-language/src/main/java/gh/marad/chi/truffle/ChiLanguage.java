package gh.marad.chi.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import gh.marad.chi.core.Compiler;
import gh.marad.chi.core.Level;
import gh.marad.chi.truffle.compilation.CompilationFailed;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.nodes.expr.BlockExpr;

@TruffleLanguage.Registration(id = "chi", name = "Chi")
public class ChiLanguage extends TruffleLanguage<ChiContext> {
    public static final String name = "chi";
    private static final LanguageReference<ChiLanguage> REFERENCE = LanguageReference.create(ChiLanguage.class);
    public static ChiLanguage get(Node node) { return REFERENCE.get(node); }

    @Override
    protected ChiContext createContext(Env env) {
        return new ChiContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        var context = ChiContext.get(null);
        var source = request.getSource();
        var sourceString = source.getCharacters().toString();
        var compiled = Compiler.compile(sourceString, context.globalCompilationScope);

        if (compiled.hasErrors()) {
            compiled.getMessages().forEach(message -> {
                var msgStr = Compiler.formatCompilationMessage(sourceString, message);
                if (message.getLevel() == Level.ERROR) {
                    System.err.println(msgStr);
                } else {
                    System.out.println(msgStr);
                }
            });
            throw new CompilationFailed(compiled.getMessages());
        }

        var fdBuilder = FrameDescriptor.newBuilder();
        var converter = new Converter(this, context.globalScope, fdBuilder);
        var executableAst = (BlockExpr) converter.convertProgram(compiled.getProgram());
        var rootNode = new FnRootNode(this, fdBuilder.build(), executableAst);
        return rootNode.getCallTarget();
    }
}
