package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

@NodeField(name = "name", type = String.class)
@NodeChild(value = "valueNode", type = ChiNode.class)
public abstract class WriteOuterVariable extends ExpressionNode {
    protected abstract String getName();

    @Specialization(guards = "isLongOrIllegal(frame)")
    protected long writeLong(VirtualFrame frame, long value) {
        findScope(frame, getName()).setLong(getName(),value);
        return value;
    }

    @Specialization(guards = "isFloatOrIllegal(frame)")
    protected float writeFloat(VirtualFrame frame, float value) {
        findScope(frame, getName()).setFloat(getName(),value);
        return value;
    }

    @Specialization(guards = "isBooleanOrIllegal(frame)")
    protected boolean writeBoolean(VirtualFrame frame, boolean value) {
        findScope(frame, getName()).setBoolean(getName(),value);
        return value;
    }

    @Specialization(replaces = {"writeLong", "writeFloat", "writeBoolean"})
    protected Object write(VirtualFrame frame, Object value) {
        findScope(frame, getName()).setObject(getName(),value);
        return value;
    }

    private LexicalScope findScope(VirtualFrame frame, String name) {
        var currentScope = getParentScope(frame);

        while(true) {
            var slot = currentScope.findSlot(name);
            if (slot != -1) {
                return currentScope;
            }
            currentScope = currentScope.getParentScope();
            if (currentScope == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("Variable %s cannot be found in the outer scopes".formatted(name));
            }
        }
    }

    protected boolean isLongOrIllegal(VirtualFrame frame) {
        final var kind = findScope(frame, getName()).getSlotKind(getName());
        return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
    }

    protected boolean isFloatOrIllegal(VirtualFrame frame) {
        final var kind = findScope(frame, getName()).getSlotKind(getName());
        return kind == FrameSlotKind.Float || kind == FrameSlotKind.Illegal;
    }

    protected boolean isBooleanOrIllegal(VirtualFrame frame) {
        final var kind = findScope(frame, getName()).getSlotKind(getName());
        return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
    }
}
