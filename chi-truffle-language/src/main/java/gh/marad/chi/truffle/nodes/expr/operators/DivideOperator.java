package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class DivideOperator extends BinaryOperator {
    @Specialization
    public long doLongs(long left, long right) { return Math.floorDiv(left, right); };

    @Specialization
    public float doFloats(float left, float right) { return left / right; }
}
