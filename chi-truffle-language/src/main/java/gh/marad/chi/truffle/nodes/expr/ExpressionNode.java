package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.instrumentation.*;
import gh.marad.chi.truffle.nodes.ChiNode;

@GenerateWrapper
public class ExpressionNode extends ChiNode implements InstrumentableNode {
    @Override
    public boolean isInstrumentable() {
        return false;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ExpressionNodeWrapper(this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.ExpressionTag.class;
    }
}
