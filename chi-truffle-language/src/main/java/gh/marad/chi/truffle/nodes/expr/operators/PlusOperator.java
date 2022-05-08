package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class PlusOperator extends BinaryOperator {
    @Specialization
    public long doLongs(long left, long right) { return Math.addExact(left, right); };

    @Specialization
    public float doFloats(float left, float right) { return left + right; }

    @Specialization
    public String doStrings(String left, String right) { return left + right; }

    @Specialization(guards = "isString(left, right)")
    public String doStrings(Object left, Object right) { return left.toString() + right.toString(); }

    boolean isString(Object left, Object right) { return left instanceof String || right instanceof String; }
}
