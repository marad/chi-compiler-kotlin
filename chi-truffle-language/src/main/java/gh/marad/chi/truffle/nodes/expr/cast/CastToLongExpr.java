package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class CastToLongExpr extends CastExpression {
    @Specialization
    long fromLong(long value) {
        return value;
    }

    @Specialization
    long fromFloat(float value) {
        return (long) value;
    }

    @Specialization
    long fromString(String value) {
        return Integer.parseInt(value);
    }
}
