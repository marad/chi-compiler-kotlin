package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import gh.marad.chi.truffle.ChiArgs;

public class LexicalScope implements TruffleObject {
    private final MaterializedFrame frame;
    private final String[] slots;

    @CompilerDirectives.CompilationFinal
    private static LexicalScope emptyInstance = null;

    public static LexicalScope empty() {
        if (emptyInstance == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            emptyInstance = new LexicalScope(Truffle.getRuntime().createMaterializedFrame(new Object[0]));
        }
        return emptyInstance;
    }

    public LexicalScope(MaterializedFrame frame) {
        this.frame = frame;
        var fd = frame.getFrameDescriptor();
        slots = new String[fd.getNumberOfSlots()];
        for (int i = 0; i < fd.getNumberOfSlots(); i++) {
            slots[i] = (String) fd.getSlotName(i);
        }
    }

    public LexicalScope getParentScope() {
        return ChiArgs.getParentScope(frame);
    }

    public void setLong(String name, long value) {
        var slot = findSlot(name);
        assert slot >= 0 : "This scope doesn't have variable '%s'".formatted(name);
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Long);
        frame.setLong(slot, value);
    }

    public void setFloat(String name, float value) {
        var slot = findSlot(name);
        assert slot >= 0 : "This scope doesn't have variable '%s'".formatted(name);
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Float);
        frame.setFloat(slot, value);
    }

    public void setBoolean(String name, boolean value) {
        var slot = findSlot(name);
        assert slot >= 0 : "This scope doesn't have variable '%s'".formatted(name);
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
    }

    public void setObject(String name, Object value) {
        var slot = findSlot(name);
        assert slot >= 0 : "This scope doesn't have variable '%s'".formatted(name);
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Object);
        frame.setObject(slot, value);
    }

    public Object getValue(String name) {
        var slot = findSlot(name);
        if (slot == -1) {
            return null;
        }
        return frame.getValue(slot);
    }

    public int findSlot(String name) {
        var i = 0;
        for (var slotName : slots) {
            if (name.equals(slotName)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public FrameSlotKind getSlotKind(String name) {
        var slot = findSlot(name);
        assert slot >= 0 : "This scope doesn't have variable '%s'".formatted(name);
        return frame.getFrameDescriptor().getSlotKind(slot);
    }

    public Frame getFrame() {
        return frame;
    }

}
