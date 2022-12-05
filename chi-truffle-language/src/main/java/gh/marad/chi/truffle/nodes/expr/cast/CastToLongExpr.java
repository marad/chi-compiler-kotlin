package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class CastToLongExpr extends CastExpression {

    @Specialization
    long fromInt(int value) {
        return value;
    }

    @Specialization
    long fromLong(long value) {
        return value;
    }

    @Specialization
    long fromFloat(float value) {
        return (long) value;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    long fromString(String value) {
        return Integer.parseInt(value);
    }
}
