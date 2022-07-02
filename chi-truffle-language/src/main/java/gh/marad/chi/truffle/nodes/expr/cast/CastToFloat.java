package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

public class CastToFloat extends CastExpression {
    @Specialization
    double fromLong(long value) {
        return (double) value;
    }

    @Specialization
    double fromDouble(double value) {
        return value;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    double fromString(TruffleString value,
                     @Cached("createDoubleParserNode()") TruffleString.ParseDoubleNode parser) {
        try {
            return parser.execute(value);
        } catch (TruffleString.NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    protected TruffleString.ParseDoubleNode createDoubleParserNode() {
        return TruffleString.ParseDoubleNode.create();
    }
}
