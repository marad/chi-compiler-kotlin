package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.NodeChild;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

@NodeChild(value = "left", type = ChiNode.class)
@NodeChild(value = "right", type = ChiNode.class)
public abstract class BinaryOperator extends ExpressionNode {

}
