package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;

public class EqualOperator extends BinaryOperator {
    @Specialization
    public boolean doLongs(long left, long right) {
        return left == right;
    }

    @Specialization
    public boolean doFloats(float left, float right) {
        return left == right;
    }

    @Specialization
    public boolean doBooleans(boolean left, boolean right) {
        return left == right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public boolean doStrings(String left, String right) {
        return left.equals(right);
    }
}
