package gh.marad.chi.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiArray;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.ChiObject;

@TypeSystem({long.class, float.class, boolean.class, TruffleString.class, ChiFunction.class, ChiObject.class})
public class ChiTypes {

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(int i) {
        return TruffleString.fromLongUncached(i, TruffleString.Encoding.UTF_8, false);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(long l) {
        return TruffleString.fromLongUncached(l, TruffleString.Encoding.UTF_8, false);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(float f) {
        return TruffleString.fromJavaStringUncached(Float.toString(f), TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(String s) {
        return TruffleString.fromJavaStringUncached(s, TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(boolean b) {
        return TruffleString.fromJavaStringUncached(Boolean.toString(b), TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(ChiObject o) {
        return TruffleString.fromJavaStringUncached(
                (String) o.toDisplayString(false, DynamicObjectLibrary.getUncached(), InteropLibrary.getUncached()),
                TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static TruffleString toTruffleString(ChiArray arr) {
        return TruffleString.fromJavaStringUncached((String) arr.toDisplayString(false), TruffleString.Encoding.UTF_8);
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static String truffleStringToString(TruffleString s) {
        return s.toString();
    }


    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static long toLong(int i) {
        return i;
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static long toLong(Integer i) {
        return i;
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static float toFloat(double d) {
        return (float) d;
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static float toFloat(Double d) {
        return (float) d.doubleValue();
    }

    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static ChiArray toChiArray(Object[] arr) {
        return new ChiArray(arr);
    }
}
