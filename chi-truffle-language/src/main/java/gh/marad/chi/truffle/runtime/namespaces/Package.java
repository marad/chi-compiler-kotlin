package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.runtime.ChiFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

public class Package {
    private final String name;
    private final HashMap<FunctionKey, ChiFunction> functions;
    private final HashMap<String, Object> variables;

    public Package(String name) {
        this.name = name;
        this.functions = new HashMap<>();
        this.variables = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    @CompilerDirectives.TruffleBoundary
    public Iterable<ChiFunction> listFunctions() {
        return functions.values();
    }

    @CompilerDirectives.TruffleBoundary
    public void defineFunction(ChiFunction function, Type[] paramTypes) {
        var key = new FunctionKey(function.getExecutableName(), Objects.hash((Object[]) paramTypes));
        functions.put(key, function);
    }

    @CompilerDirectives.TruffleBoundary
    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable ChiFunction findFunctionOrNull(String name, Type[] paramTypes) {
        var key = new FunctionKey(name, Objects.hash((Object[]) paramTypes));
        return functions.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable Object findVariableOrNull(String name) {
        return variables.get(name);
    }

    public record FunctionKey(String name, int paramTypesHash) {
    }
}
