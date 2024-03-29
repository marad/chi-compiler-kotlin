package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.runtime.LexicalScope;

@GenerateWrapper
public abstract class ScopedNode extends ChiNode implements InstrumentableNode {

    public LexicalScope getParentScope(Frame frame) {
        if (frame.getArguments().length > 0) {
            return ChiArgs.getParentScope(frame);
        } else {
            return ChiContext.get(this).globalScope;
        }
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ScopedNodeWrapper(this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return super.hasTag(tag);
    }
}
