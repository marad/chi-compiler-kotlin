package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;

import java.text.DecimalFormat;

public abstract class CastToString extends CastExpression {

    private final TruffleString.FromLongNode fromLongNode = TruffleString.FromLongNode.create();
    private final TruffleString.FromJavaStringNode fromJavaStringNode = TruffleString.FromJavaStringNode.create();
    private final DecimalFormat df = new DecimalFormat("#.#");

    @Specialization
    @CompilerDirectives.TruffleBoundary
    TruffleString fromLong(long value) {
        return fromLongNode.execute(value, TruffleString.Encoding.UTF_8, false);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    TruffleString fromFloat(float value) {
        return fromJavaStringNode.execute(df.format(value), TruffleString.Encoding.UTF_8);
    }

    @Specialization
    TruffleString fromString(String value) {
        return fromJavaStringNode.execute(value, TruffleString.Encoding.UTF_8);
    }

    @Specialization
    TruffleString fromBoolean(boolean value) {
        if (value) {
            return fromJavaStringNode.execute("true", TruffleString.Encoding.UTF_8);
        } else {
            return fromJavaStringNode.execute("false", TruffleString.Encoding.UTF_8);
        }
    }

    @Fallback
    TruffleString fromInteropValue(Object value,
                                   @CachedLibrary(limit = "3") InteropLibrary interop) {
        return fromJavaStringNode.execute((String) interop.toDisplayString(value), TruffleString.Encoding.UTF_8);
    }
}
