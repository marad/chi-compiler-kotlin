package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperatorWithFallback;

public class LessThanOperator extends BinaryOperatorWithFallback {
    private final boolean inclusive;

    public LessThanOperator(boolean inclusive) {
        this.inclusive = inclusive;
    }

    @Specialization
    public boolean doLongs(long left, long right) {
        if (inclusive) {
            return left <= right;
        } else {
            return left < right;
        }
    }

    @Specialization
    public boolean doFloats(float left, float right) {
        if (inclusive) {
            return left <= right;
        } else {
            return left < right;
        }
    }
}
