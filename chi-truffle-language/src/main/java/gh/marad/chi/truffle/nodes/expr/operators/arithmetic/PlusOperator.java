package gh.marad.chi.truffle.nodes.expr.operators.arithmetic;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperatorWithFallback;

public abstract class PlusOperator extends BinaryOperatorWithFallback {
    @Specialization(rewriteOn = ArithmeticException.class)
    public long doLongs(long left, long right) { return Math.addExact(left, right); }

    @Specialization
    public double doDoubles(double left, double right) { return left + right; }

    @Specialization
    public TruffleString doStrings(TruffleString left, TruffleString right,
                                   @Cached("createConcatNode()") TruffleString.ConcatNode concatNode) {
        return concatNode.execute(left, right, TruffleString.Encoding.UTF_8, true);
    }

    // TODO: implement for string + other and other + string
//    @Specialization(guards = "isString(left, right)")
//    public TruffleString doStrings(Object left, Object right,
//                                   @Cached("createConcatNode()") TruffleString.ConcatNode concatNode) {
//        return
//    }
//
//    boolean isString(Object left, Object right) { return left instanceof TruffleString || right instanceof TruffleString; }

    protected TruffleString.ConcatNode createConcatNode() {
        return TruffleString.ConcatNode.create();
    }
}
