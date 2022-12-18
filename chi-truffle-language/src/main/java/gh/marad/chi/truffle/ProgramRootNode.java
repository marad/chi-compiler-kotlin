package gh.marad.chi.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.Unit;

public class ProgramRootNode extends RootNode {
    @Child
    private ChiNode body;
    private final FrameDescriptor frameDescriptor;

    protected ProgramRootNode(TruffleLanguage<?> language, ChiNode body, FrameDescriptor frameDescriptor) {
        super(language);
        var source = Source.newBuilder("chi", "foo", "dummy.chi").build();
        this.body = body;
        this.frameDescriptor = frameDescriptor;
    }

    @Override
    public String getName() {
        return "[root]";
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var globalScope = ChiContext.get(this).globalScope;
        var mainFrame = Truffle.getRuntime().createVirtualFrame(
                ChiArgs.create(globalScope), frameDescriptor);
        try {
            return body.executeGeneric(mainFrame);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            for (TruffleStackTraceElement element : TruffleStackTrace.getStackTrace(ex)) {
                var functionName = element.getTarget().getRootNode().getName();
                var source = element.getTarget().getRootNode().getSourceSection();
                String location = "";

                if (source != null) {
                    var fileName = source.getSource().getName();
                    var line = source.getStartLine();
                    var col = source.getStartColumn();
                    location = " [%s %d:%d]".formatted(fileName, line, col);
                }

                System.err.printf(
                        "\t%s%s%n",
                        functionName,
                        location
                );
                System.err.flush();
            }

            Throwable cause = ex.getCause();
            while (cause != null) {
                if (cause.getMessage() != null) {
                    System.err.println("Cause: " + cause.getMessage());
                }
                cause = cause.getCause();
            }

            return Unit.instance;
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
