package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.ChiObject;

@TypeSystem({long.class, float.class, boolean.class, TruffleString.class, ChiFunction.class, ChiObject.class})
public class ChiTypes {
    @ImplicitCast
    public static TruffleString toTruffleString(String s) {
        return TruffleString.fromJavaStringUncached(s, TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    public static long toLong(int i) {
        return i;
    }

    @ImplicitCast
    public static long toLong(Integer i) {
        return i;
    }

    @ImplicitCast
    public static float toFloat(double d) {
        return (float) d;
    }

    @ImplicitCast
    public static float toFloat(Double d) {
        return (float) d.doubleValue();
    }
}
