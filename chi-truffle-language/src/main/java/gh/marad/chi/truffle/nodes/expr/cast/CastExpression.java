package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

@NodeChild(value = "value", type = ChiNode.class)
public abstract class CastExpression extends ExpressionNode {
}
