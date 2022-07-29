package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;

import java.text.DecimalFormat;

public class CastToString extends CastExpression {
    private final DecimalFormat df = new DecimalFormat("#.#");
    @Specialization
    @CompilerDirectives.TruffleBoundary
    String fromLong(long value) {
        return String.format("%d", value);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    String fromFloat(float value) {
        return df.format(value);
    }

    @Specialization
    String fromString(String value) {
        return value;
    }
}
