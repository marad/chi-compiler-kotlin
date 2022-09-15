package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.TODO;

public class CastToLongExpr extends CastExpression {

    @Child
    private TruffleString.ParseLongNode parseLongNode;

    public CastToLongExpr() {
        this.parseLongNode = TruffleString.ParseLongNode.create();
    }

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

    @Specialization
    long fromTruffleString(TruffleString value) {
        try {
            return parseLongNode.execute(value, 10);
        } catch (TruffleString.NumberFormatException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }
}
