package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.ChiStaticObject;

@TypeSystem({long.class, float.class, boolean.class, TruffleString.class, ChiFunction.class, ChiStaticObject.class})
public class ChiTypes {
}
