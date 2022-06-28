package gh.marad.chi.truffle.runtime.namespaces;

import gh.marad.chi.truffle.runtime.ChiFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class Package {
    private final String name;
    private final HashMap<String, ChiFunction> functions;

    public Package(String name) {
        this.name = name;
        this.functions = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Iterable<ChiFunction> listFunctions() {
        return functions.values();
    }

    public void defineFunction(ChiFunction function) {
        functions.put(function.getExecutableName(), function);
    }

    public @Nullable ChiFunction findFunctionOrNull(String name) {
        return functions.get(name);
    }

}
