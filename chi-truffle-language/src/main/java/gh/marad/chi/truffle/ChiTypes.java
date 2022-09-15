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
}
