package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.runtime.Unit;

public class UnitValue extends ValueNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return Unit.instance;
    }
}
