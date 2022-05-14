package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.value.LambdaValue;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.TODO;

public class ReadVariableExpr extends ChiNode {
    private final String name;
    private final LexicalScope scope;
    private final int slot;

    public ReadVariableExpr(String name, LexicalScope scope, int slot) {
        this.name = name;
        this.scope = scope;
        this.slot = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
//        var value = scope.getValue(name);
//        var value = frame.getValue(slot);
        var value = findVariableValue(name);
        if (ChiTypesGen.isChiFunction(value)) {
            replace(new LambdaValue((ChiFunction) value), "cache and skip scope lookup for functions");
        }
        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO("Undefined name");
        }
        return value;
    }

    public Object findVariableValue(String name) {
        return Truffle.getRuntime().iterateFrames(frameInstance -> {
            var frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            var desc = frame.getFrameDescriptor();
            for (var i = 0; i < desc.getNumberOfSlots(); i++) {
                if (name.equals(desc.getSlotName(i))) {
                    return frame.getValue(i);
                }
            }
            return null;
        });
    }
}
