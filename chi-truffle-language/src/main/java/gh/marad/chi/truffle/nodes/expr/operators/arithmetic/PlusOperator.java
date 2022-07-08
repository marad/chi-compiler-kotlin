package gh.marad.chi.truffle.nodes.expr.operators.arithmetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperatorWithFallback;

public abstract class PlusOperator extends BinaryOperatorWithFallback {
    @Specialization
    public long doLongs(long left, long right) { return Math.addExact(left, right); }

    @Specialization
    public float doFloats(float left, float right) { return left + right; }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String doStrings(String left, String right) {
        return String.format("%s%s", left, right);
    }

//    @Specialization(guards = "isString(left, right)")
//    public String doStrings(Object left, Object right) { return left.toString() + right.toString(); }
//
//    boolean isString(Object left, Object right) { return left instanceof String || right instanceof String; }
}
