package gh.marad.chi.truffle.builtin.lang.interop;

import gh.marad.chi.truffle.builtin.Builtin;

public abstract class LangInteropBuiltin extends Builtin {
    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "lang.interop";
    }

}
