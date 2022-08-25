package gh.marad.chi.truffle.builtin.collections;

import gh.marad.chi.truffle.builtin.Builtin;

public abstract class CollectionsBuiltin extends Builtin {
    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "collections";
    }
}
