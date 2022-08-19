package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;
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

    public Iterable<ChiFunction> listFunctions(String packageName) {
        return getPackage(packageName)
                       .listFunctions();
    }

    @CompilerDirectives.TruffleBoundary
    public void defineFunction(@NotNull String packageName, @NotNull ChiFunction function) {
        getOrCreatePackage(packageName)
                .defineFunction(function);
    }

    public @Nullable ChiFunction findFunctionOrNull(@NotNull String packageName, @NotNull String functionName) {
        return getPackage(packageName)
                       .findFunctionOrNull(functionName);
    }

    public void defineVariable(@NotNull String packageName, @NotNull String name, @NotNull Object value) {
        getOrCreatePackage(packageName)
                .defineVariable(name, value);
    }

    public @Nullable Object findVariableOrNull(@NotNull String packageName, @NotNull String variableName) {
        return getPackage(packageName)
                       .findVariableOrNull(variableName);
    }

    public void defineObjectDescriptor(@NotNull String packageName, @NotNull ChiObjectDescriptor descriptor) {
        getOrCreatePackage(packageName)
                .defineObjectDescriptor(descriptor.getTypeName(), descriptor);
    }

    public @Nullable ChiObjectDescriptor getObjectDescriptorOrNull(@NotNull String packageName, @NotNull String typeName) {
        return getPackage(packageName).findObjectDescriptor(typeName);
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
