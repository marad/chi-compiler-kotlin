package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.runtime.ChiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class Module {
    private final String name;
    private final HashMap<String, Package> packages;

    public Module(String name) {
        this.name = name;
        this.packages = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    @CompilerDirectives.TruffleBoundary
    public boolean packageExists(String packageName) {
        return packages.containsKey(packageName);
    }

    @CompilerDirectives.TruffleBoundary
    public Iterable<String> listPackages() {
        return packages.keySet();
    }

    public Iterable<Package.FunctionLookupResult> listFunctions(String packageName) {
        return getPackage(packageName)
                       .listFunctions();
    }

    @CompilerDirectives.TruffleBoundary
    public void defineFunction(@NotNull String packageName, @NotNull ChiFunction function, Type[] paramTypes) {
        getOrCreatePackage(packageName)
                .defineFunction(function, paramTypes);
    }

    @CompilerDirectives.TruffleBoundary
    public void defineNamedFunction(@NotNull String packageName, @NotNull String name, @NotNull ChiFunction function, Type[] paramTypes) {
        getOrCreatePackage(packageName)
                .defineNamedFunction(name, function, paramTypes);
    }

    public @Nullable Package.FunctionLookupResult findFunctionOrNull(@NotNull String packageName, @NotNull String functionName, Type[] paramTypes) {
        return getPackage(packageName)
                       .findFunctionOrNull(functionName, paramTypes);
    }

    public void defineVariable(@NotNull String packageName, @NotNull String name, @NotNull Object value) {
        getOrCreatePackage(packageName)
                .defineVariable(name, value);
    }

    public @Nullable Object findVariableFunctionOrNull(@NotNull String packageName, @NotNull String symbolName) {
        var pkg = getPackage(packageName);
        var variable = pkg.findVariableOrNull(symbolName);
        if (variable != null) {
            return variable;
        }
        // FIXME: proper lookup with overloaded functions
        var functionLookup = pkg.findSingleFunctionOrNull(symbolName);
        if (functionLookup != null) {
            return functionLookup.function();
        }
        return null;
    }

    @CompilerDirectives.TruffleBoundary
    private Package getOrCreatePackage(String packageName) {
        var pkg = packages.get(packageName);
        if (pkg != null) {
            return pkg;
        } else {
            var newPackage = new Package(packageName);
            packages.put(newPackage.getName(), newPackage);
            return newPackage;
        }
    }

    @CompilerDirectives.TruffleBoundary
    private Package getPackage(String packageName) {
        var pkg = packages.get(packageName);
        if (pkg == null) {
            CompilerDirectives.transferToInterpreter();
            throw new NoSuchPackageException(packageName);
        } else {
            return pkg;
        }
    }
}
