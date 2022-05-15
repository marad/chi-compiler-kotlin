package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class FnRootNode extends RootNode {
    @Child private ChiNode body;

    public FnRootNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ExpressionNode body) {
        super(language, frameDescriptor);
        this.body = body;
        body.addRootTag();
    }

    @Override
    public String getName() {
        return "foo";
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }

}
