package gh.marad.chi.truffle.runtime;

public class Unit extends ChiValue {
    private Unit() {
    }

    public static final Unit instance = new Unit();

    @Override
    public Object toDisplayString(boolean allowSideEffects) {
        return "()";
    }
}
