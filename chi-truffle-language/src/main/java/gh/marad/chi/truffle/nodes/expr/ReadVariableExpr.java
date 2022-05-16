package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.NodeInfo;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.nodes.value.LambdaValue;
import gh.marad.chi.truffle.runtime.ChiFunction;
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
        var value = findVariableValue(name);
        if (ChiTypesGen.isChiFunction(value)) {
//            CompilerDirectives.transferToInterpreterAndInvalidate();
            CompilerDirectives.transferToInterpreter();
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
