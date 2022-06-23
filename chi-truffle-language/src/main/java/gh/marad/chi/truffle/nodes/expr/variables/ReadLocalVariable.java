package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.NodeInfo;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

@NodeInfo(language = ChiLanguage.id, description = "Reads a variable")
@GenerateWrapper
public class ReadLocalVariable extends ExpressionNode implements InstrumentableNode {
    private final String name;
    private final int slot;

    public ReadLocalVariable(String name, int slot) {
        this.name = name;
        this.slot = slot;
    }

    public ReadLocalVariable(ReadLocalVariable expr) {
        this.name = expr.name;
        this.slot = expr.slot;
    }

    @Override
    public long executeLong(VirtualFrame frame) {
        return frame.getLong(slot);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return frame.getBoolean(slot);
    }

    @Override
    public float executeFloat(VirtualFrame frame) {
        return frame.getFloat(slot);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getValue(slot);
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ReadLocalVariableWrapper(this, this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.ReadVariableTag.class || super.hasTag(tag);
    }
}
