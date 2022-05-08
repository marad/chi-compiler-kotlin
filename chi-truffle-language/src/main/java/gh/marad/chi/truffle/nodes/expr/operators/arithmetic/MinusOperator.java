package gh.marad.chi.truffle.nodes.expr.operators.arithmetic;

import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;

public abstract class MinusOperator extends BinaryOperator {
    @Specialization
    public long doLongs(long left, long right) { return Math.subtractExact(left, right); }

    @Specialization
    public float doFloats(float left, float right) { return left - right; }
}
