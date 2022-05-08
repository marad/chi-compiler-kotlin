package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import gh.marad.chi.truffle.nodes.ChiNode;

@NodeChild("left")
@NodeChild("right")
public abstract class BinaryOperator extends ChiNode {

    @Fallback
    public void fallback(Object left, Object right) {
        throw new RuntimeException("Unexpected %s and %s for operator".formatted(left, right));
    }
}
