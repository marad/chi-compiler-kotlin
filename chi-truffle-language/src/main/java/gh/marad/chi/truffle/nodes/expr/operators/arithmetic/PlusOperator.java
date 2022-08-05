package gh.marad.chi.truffle.nodes.expr.operators.arithmetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperatorWithFallback;

public abstract class PlusOperator extends BinaryOperatorWithFallback {
    @Specialization
    public long doLongs(long left, long right) {
        return Math.addExact(left, right);
    }

    @Specialization
    public float doFloats(float left, float right) {
        return left + right;
    }

    @Specialization
    public TruffleString doTruffleStrings(TruffleString left, TruffleString right,
                                          @Cached TruffleString.ConcatNode concatNode) {
        return concatNode.execute(left, right, TruffleString.Encoding.UTF_8, false);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public TruffleString doTruffleStringLeft(TruffleString left, Object right,
                                             @Cached TruffleString.ConcatNode concatNode) {
        var rightString = TruffleString.fromJavaStringUncached(right.toString(), TruffleString.Encoding.UTF_8);
        return concatNode.execute(left, rightString, TruffleString.Encoding.UTF_8, false);
    }
}
