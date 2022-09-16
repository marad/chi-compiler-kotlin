package gh.marad.chi.truffle.nodes.expr.operators.bit;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class BitOrOperator extends BitOperator {
    @Specialization
    public long doLongs(long left, long right) {
        return left | right;
    }
}
