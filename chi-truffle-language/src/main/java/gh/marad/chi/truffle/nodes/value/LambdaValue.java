package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.runtime.ChiFunction;

public class LambdaValue extends ValueNode {
    private final ChiFunction function;

    public LambdaValue(ChiFunction function) {
        this.function = function;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        return function;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return function;
    }
}
