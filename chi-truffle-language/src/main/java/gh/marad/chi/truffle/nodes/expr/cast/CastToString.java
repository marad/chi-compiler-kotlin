package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

import java.text.DecimalFormat;

public class CastToString extends CastExpression {
    private final DecimalFormat df = new DecimalFormat("#.#");
    @Specialization
    TruffleString fromLong(long value) {
        return TruffleString.fromLongUncached(value, TruffleString.Encoding.UTF_8, false);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    TruffleString fromDouble(double value) {
        return TruffleString.fromJavaStringUncached(df.format(value), TruffleString.Encoding.UTF_8);
    }

    @Specialization
    TruffleString fromString(TruffleString value) {
        return value;
    }
}
