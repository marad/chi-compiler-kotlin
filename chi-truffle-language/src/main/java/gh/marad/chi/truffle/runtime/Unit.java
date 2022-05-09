package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.interop.TruffleObject;

public class Unit implements TruffleObject {
    private Unit() {}
    public static final Unit instance = new Unit();
}
