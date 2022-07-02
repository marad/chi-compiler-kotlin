package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;

public class EqualOperator extends BinaryOperator {
    @Specialization
    public boolean doLongs(long left, long right) {
        return left == right;
    }

    @Specialization
    public boolean doDoubles(double left, double right) {
        return left == right;
    }

    @Specialization
    boolean doBooleans(boolean left, boolean right) {
        return left == right;
    }

    @Specialization
    boolean doStrings(TruffleString left, TruffleString right,
                      @Cached("createEqualNode()") TruffleString.EqualNode equalNode) {
        return equalNode.execute(left, right, TruffleString.Encoding.UTF_8);
    }

    protected TruffleString.EqualNode createEqualNode() {
        return TruffleString.EqualNode.create();
    }

    // TODO: implement for the other types
//    @Specialization
//    public boolean doOther(Object left, Object right) {
//        return left.equals(right); // this is blacklisted and cannot be used
//    }
}
