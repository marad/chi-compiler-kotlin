package gh.marad.chi.truffle.builtin.lang.usafe;

import gh.marad.chi.truffle.builtin.Builtin;

public abstract class LangUnsafeBuiltin extends Builtin {
    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "lang.unsafe";
    }

}
