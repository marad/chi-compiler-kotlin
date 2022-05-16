package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import gh.marad.chi.truffle.runtime.TODO;

@ExportLibrary(NodeLibrary.class)
@GenerateWrapper
public class ScopedNode extends ChiNode implements InstrumentableNode {
    private boolean hasRootTag = false;
    public void addRootTag() {
        hasRootTag = true;
    }

    @ExportMessage
    public boolean hasScope(Frame frame) {
        throw new TODO();
    }

    @ExportMessage
    public final Object getScope(Frame frame, boolean nodeEnter) throws UnsupportedMessageException {
        throw new TODO();
    }

    @ExportMessage
    public boolean hasRootInstance(Frame frame) {
        throw new TODO();
    }

    @ExportMessage
    public Object getRootInstance(Frame frame) {
        throw new TODO();
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
        return hasRootTag && (tag == StandardTags.RootTag.class || tag == StandardTags.RootBodyTag.class);
    }

}
