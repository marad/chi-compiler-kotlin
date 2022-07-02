package gh.marad.chi.truffle.nodes.expr.operators.arithmetic;

import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperatorWithFallback;

public abstract class MultiplyOperator extends BinaryOperatorWithFallback {
    @Specialization
    public long doLongs(long left, long right) { return Math.multiplyExact(left, right); }

    @Specialization
    public double doDoubles(double left, double right) { return left * right; }
}
