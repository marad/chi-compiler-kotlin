package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;

@NodeChild("value")
public class LogicNotOperator extends ChiNode {
    @Specialization
    public boolean doBoolean(boolean value) {
        return !value;
    }
}
