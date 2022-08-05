package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;
import gh.marad.chi.truffle.runtime.ChiFunction;
import com.oracle.truffle.api.strings.TruffleString;

@TypeSystem({long.class, float.class, boolean.class, TruffleString.class, ChiFunction.class})
public class ChiTypes {
}
