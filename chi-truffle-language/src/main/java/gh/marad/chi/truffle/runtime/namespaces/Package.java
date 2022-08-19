package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class Package {
    private final String name;
    private final HashMap<String, ChiFunction> functions;
    private final HashMap<String, Object> variables;

    private final HashMap<String, ChiObjectDescriptor> objectDescriptors;

    public Package(String name) {
        this.name = name;
        this.functions = new HashMap<>();
        this.variables = new HashMap<>();
        this.objectDescriptors = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    @CompilerDirectives.TruffleBoundary
    public Iterable<ChiFunction> listFunctions() {
        return functions.values();
    }

    @CompilerDirectives.TruffleBoundary
    public void defineFunction(ChiFunction function) {
        functions.put(function.getExecutableName(), function);
    }

    @CompilerDirectives.TruffleBoundary
    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }

    @CompilerDirectives.TruffleBoundary
    public void defineObjectDescriptor(String name, ChiObjectDescriptor descriptor) {
        objectDescriptors.put(name, descriptor);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable ChiFunction findFunctionOrNull(String name) {
        return functions.get(name);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable Object findVariableOrNull(String name) {
        return variables.get(name);
    }

    @CompilerDirectives.TruffleBoundary
    public @Nullable ChiObjectDescriptor findObjectDescriptor(String name) {
        return objectDescriptors.get(name);
    }
}
