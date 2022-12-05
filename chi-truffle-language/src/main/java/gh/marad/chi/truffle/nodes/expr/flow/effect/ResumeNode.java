package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.FnRootNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

public class ResumeNode extends ExpressionNode {

    private ResumeNode() {
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var value = ChiArgs.getObject(frame, 0);
        throw new ResumeValueException(value);
    }

    public static ChiFunction createResumeFunction(ChiLanguage language) {
        var rootNode = new FnRootNode(language, FrameDescriptor.newBuilder().build(), new ResumeNode(), "resume");
        return new ChiFunction(rootNode.getCallTarget());
    }
}
