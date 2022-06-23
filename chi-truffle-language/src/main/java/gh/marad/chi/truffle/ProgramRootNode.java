package gh.marad.chi.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ProgramRootNode extends RootNode {
    @Child private ChiNode body;
    private final SourceSection sourceSection;
    private final FrameDescriptor frameDescriptor;
    protected ProgramRootNode(TruffleLanguage<?> language, ExpressionNode body, FrameDescriptor frameDescriptor) {
        super(language);
        var source = Source.newBuilder("chi", "foo", "dummy.chi").build();
        this.sourceSection = source.createSection(1);
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
        return body.executeGeneric(mainFrame);
    }

    @Override
    public String toString() {
        return getName();
    }
}
