package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;

public class ConstructType extends ExpressionNode {
    private final ChiObjectDescriptor descriptor;

    public ConstructType(ChiObjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        return descriptor.constructObject(frame);
    }


    public record Field(String name, Class<?> type) {
    }
}
