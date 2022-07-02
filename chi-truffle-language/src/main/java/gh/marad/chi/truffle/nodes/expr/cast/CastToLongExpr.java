package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

public class CastToLongExpr extends CastExpression {
    @Specialization
    long fromLong(long value) {
        return value;
    }

    @Specialization
    long fromDouble(double value) {
        return (long) value;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    long fromString(TruffleString value,
                    @Cached("createLongNodeParser()") TruffleString.ParseLongNode parser) {
        try {
            return parser.execute(value, 10);
        } catch (TruffleString.NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    protected TruffleString.ParseLongNode createLongNodeParser() {
        return TruffleString.ParseLongNode.create();
    }
}
