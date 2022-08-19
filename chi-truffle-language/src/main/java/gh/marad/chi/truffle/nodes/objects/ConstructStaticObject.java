package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;

public class ConstructStaticObject extends ExpressionNode {
    private final ChiObjectDescriptor descriptor;

    public ConstructStaticObject(ChiObjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return descriptor.constructObject(frame);
    }
}
