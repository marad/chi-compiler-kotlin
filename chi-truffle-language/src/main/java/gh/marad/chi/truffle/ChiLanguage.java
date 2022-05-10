package gh.marad.chi.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import gh.marad.chi.core.Compiler;
import gh.marad.chi.core.Level;
import gh.marad.chi.truffle.compilation.CompilationFailed;
import gh.marad.chi.truffle.nodes.ChiRootNode;

import java.io.BufferedReader;
import java.io.Reader;

@TruffleLanguage.Registration(id = "chi", name = "Chi")
public class ChiLanguage extends TruffleLanguage<ChiContext> {
    private static final LanguageReference<ChiLanguage> REFERENCE = LanguageReference.create(ChiLanguage.class);
    public static ChiLanguage get(Node node) { return REFERENCE.get(node); }

    @Override
    protected ChiContext createContext(Env env) {
        return new ChiContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        var context = ChiContext.get(null);
        var source = readerToString(request.getSource().getReader());
        var compiled = Compiler.compile(source, context.globalCompilationScope);

        if (compiled.hasErrors()) {
            compiled.getMessages().forEach(message -> {
                var msgStr = Compiler.formatCompilationMessage(source, message);
                if (message.getLevel() == Level.ERROR) {
                    System.err.println(msgStr);
                } else {
                    System.out.println(msgStr);
                }
            });
            throw new CompilationFailed(compiled.getMessages());
        }

        var converter = new Converter(this, context.globalScope);
        var executableAst = converter.convertProgram(compiled.getProgram());
        var frameDescriptor = FrameDescriptor.newBuilder().build();
        var rootNode = new ChiRootNode(this, frameDescriptor, executableAst);
        return rootNode.getCallTarget();
    }

    private String readerToString(Reader reader) {
        var br = new BufferedReader(reader);
        var sb = new StringBuilder();
        br.lines().forEach(sb::append);
        return sb.toString();
    }
}
