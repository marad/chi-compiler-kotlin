package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;

public abstract class EqualOperator extends BinaryOperator {
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
    public boolean doTruffleStrings(TruffleString left, TruffleString right,
                                    @Cached TruffleString.EqualNode equalNode) {
        return equalNode.execute(left, right, TruffleString.Encoding.UTF_8);
    }

    @Specialization(limit = "3")
    public boolean doObjects(Object left, Object right,
                             @CachedLibrary(value = "left") InteropLibrary leftInterop,
                             @CachedLibrary(value = "right") InteropLibrary rightInterop
    ) {
        return leftInterop.isIdentical(left, right, rightInterop);
    }
}
