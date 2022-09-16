package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;

import java.text.DecimalFormat;

public abstract class CastToString extends CastExpression {
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

    @Fallback
    String fromInteropValue(Object value,
                            @CachedLibrary(limit = "3") InteropLibrary interop) {
        return (String) interop.toDisplayString(value);
    }
}
