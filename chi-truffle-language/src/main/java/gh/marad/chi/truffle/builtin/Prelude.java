package gh.marad.chi.truffle.builtin;

import gh.marad.chi.core.namespace.PreludeImport;

import java.util.List;

public class Prelude {
    public static List<PreludeImport> imports = List.of(
            new PreludeImport("std", "collections", "array", null),
            new PreludeImport("std", "collections", "arrayOf", null),
            new PreludeImport("std", "io", "println", null),
            new PreludeImport("std", "io", "print", null),
            new PreludeImport("std", "lang", "Option", null),
            new PreludeImport("std", "lang", "Result", null)
    );
}
