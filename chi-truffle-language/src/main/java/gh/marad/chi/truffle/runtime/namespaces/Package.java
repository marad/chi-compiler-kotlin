package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.runtime.ChiFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Package {
    private final String name;
    private final HashMap<FunctionKey, FunctionLookupResult> functions;
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
    public Iterable<FunctionLookupResult> listFunctions() {
        return functions.values();
    }

    @CompilerDirectives.TruffleBoundary
    public void defineFunction(ChiFunction function, Type[] paramTypes) {
        defineNamedFunction(function.getExecutableName(), function, paramTypes);
    }

    @CompilerDirectives.TruffleBoundary
    public void defineNamedFunction(String name, ChiFunction function, Type[] paramTypes) {
        var key = new FunctionKey(name, Objects.hash((Object[]) paramTypes));
        var oldDefinition = functions.get(key);
        if (oldDefinition != null) {
            oldDefinition.assumption.invalidate();
        }
        functions.put(key, new FunctionLookupResult(function, Assumption.create("function redefined")));
    }

    @CompilerDirectives.TruffleBoundary
    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable FunctionLookupResult findFunctionOrNull(String name, Type[] paramTypes) {
        var key = new FunctionKey(name, Objects.hash((Object[]) paramTypes));
        return functions.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable FunctionLookupResult findSingleFunctionOrNull(String name) {
        return functions.entrySet().stream()
                        .filter(it -> it.getKey().name.equals(name))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable Object findVariableOrNull(String name) {
        return variables.get(name);
    }

    public record FunctionKey(String name, int paramTypesHash) {
    }

    public record FunctionLookupResult(ChiFunction function, Assumption assumption) {
    }
}
