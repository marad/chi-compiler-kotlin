package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import gh.marad.chi.truffle.ChiArgs;

public class LexicalScope implements TruffleObject {
    private final MaterializedFrame frame;
    private final String[] slots;

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

    public Object getValue(String name) {
        var slot = findSlot(name);
        if (slot == -1) {
            return null;
        }
        return frame.getValue(slot);
    }

    public int findSlot(String name) {
        var i = 0;
        for(var slotName : slots) {
            if (name.equals(slotName)) {
                return i;
            }
            i++;
        }
        return -1;
    }


}
