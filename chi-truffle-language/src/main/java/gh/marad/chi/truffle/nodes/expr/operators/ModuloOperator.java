package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class ModuloOperator extends BinaryOperator {
    @Specialization
    public long doLongs(long left, long right) { return Math.floorMod(left, right); };
}
