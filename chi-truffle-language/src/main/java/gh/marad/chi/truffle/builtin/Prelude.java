package gh.marad.chi.truffle.builtin;

import gh.marad.chi.core.PreludeImport;

import java.util.List;

public class Prelude {
    public static List<PreludeImport> imports = List.of(
            new PreludeImport("std", "collections", "array", null),
            new PreludeImport("std", "io", "println", null)
    );
}
