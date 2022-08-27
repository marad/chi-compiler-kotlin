package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.Unit;

@NodeChild(value = "value", type = ChiNode.class)
public class MatchNode extends ExpressionNode {

    @Specialization
    public Object doGeneric(Object value) {
        return Unit.instance;
    }
}