package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;
import gh.marad.chi.truffle.runtime.ChiFunction;

@TypeSystem({long.class, float.class, boolean.class, String.class, ChiFunction.class})
public class ChiTypes {
}
