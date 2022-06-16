package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class FunctionScope implements TruffleObject {
    private final MaterializedFrame frame;
    private final String[] slots;
    private final FunctionScope parentScope;

    public FunctionScope(MaterializedFrame frame) {
        this.frame = frame;

        var fd = frame.getFrameDescriptor();
        slots = new String[fd.getNumberOfSlots()];
        for (int i = 0; i < fd.getNumberOfSlots(); i++) {
            slots[i] = (String) fd.getSlotName(i);
        }

        if (frame.getArguments().length > 0) {
            parentScope = (FunctionScope) frame.getArguments()[0];
        } else {
            parentScope = null;
        }
    }

//    @ExplodeLoop
    public Object getObjectByName(String name) {
        Object result = null;
        for (int i = 0; i < slots.length; i++) {
            if (name.equals(slots[i])) {
                result = frame.getValue(i);
            }
        }
        return result;
    }

    public Object findObjectByName(String name) {
//        CompilerAsserts.compilationConstant(frame.getArguments().length);
        if (frame.getArguments().length > 0) {
            var scope = (FunctionScope) frame.getArguments()[0];
            return scope.getObjectByName(name);
        }

        var currentScope = this;
        while(currentScope != null) {

            var value = currentScope.getObjectByName(name);
            if (value != null) {
                return value;
            }

            currentScope = currentScope.parentScope;
        }

        return null;
    }
}
