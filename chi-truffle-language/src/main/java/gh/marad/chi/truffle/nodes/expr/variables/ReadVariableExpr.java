package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.nodes.function.FunctionScope;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.TODO;

@NodeInfo(language = ChiLanguage.id, description = "Reads a variable")
@GenerateWrapper
public class ReadVariableExpr extends ExpressionNode implements InstrumentableNode {
    private final String name;
    private final LexicalScope scope;
    private final int slot;

    public ReadVariableExpr(String name, LexicalScope scope, int slot) {
        this.name = name;
        this.scope = scope;
        this.slot = slot;
    }

    public ReadVariableExpr(ReadVariableExpr expr) {
        this.name = expr.name;
        this.scope = expr.scope;
        this.slot = expr.slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
//        var value = scope.getValue(name);
//        var value = frame.getValue(slot);
//        var value = findVariableValue(name);
        var value = findVariableValue(frame, name);
        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO("Undefined name");
        }
        return value;
    }

    @ExplodeLoop
    public Object findVariableValue(VirtualFrame frame, String name) {
        var fd = frame.getFrameDescriptor();
        for (int i = 0; i < fd.getNumberOfSlots(); i++) {
            if (name.equals(fd.getSlotName(i))) {
                return frame.getValue(i);
            }
        }

        if (frame.getArguments().length > 0) {
            var scope = (FunctionScope) frame.getArguments()[0];
            return scope.findObjectByName(name);
        }

        return null;
    }

//    public Object findVariableValue(String name) {
//        return Truffle.getRuntime().iterateFrames(frameInstance -> {
//            var frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
//            var desc = frame.getFrameDescriptor();
//            for (var i = 0; i < desc.getNumberOfSlots(); i++) {
//                if (name.equals(desc.getSlotName(i))) {
//                    return frame.getValue(i);
//                }
//            }
//            return null;
//        });
//    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ReadVariableExprWrapper(this, this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.ReadVariableTag.class || super.hasTag(tag);
    }
}
