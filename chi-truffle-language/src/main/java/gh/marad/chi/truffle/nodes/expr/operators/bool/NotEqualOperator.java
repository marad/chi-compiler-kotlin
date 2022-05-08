package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;

public class NotEqualOperator extends BinaryOperator {
    @Specialization
    public boolean doLongs(long left, long right) {
        return left != right;
    }

    @Specialization
    public boolean doFloats(float left, float right) {
        return left != right;
    }

    @Specialization
    public boolean doOther(Object left, Object right) {
        return !left.equals(right);
    }
}
