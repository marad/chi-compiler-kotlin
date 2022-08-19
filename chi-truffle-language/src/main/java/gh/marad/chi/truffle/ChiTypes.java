package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiDynamicObject;
import gh.marad.chi.truffle.runtime.ChiFunction;

@TypeSystem({long.class, float.class, boolean.class, TruffleString.class, ChiFunction.class, ChiDynamicObject.class})
public class ChiTypes {
}
