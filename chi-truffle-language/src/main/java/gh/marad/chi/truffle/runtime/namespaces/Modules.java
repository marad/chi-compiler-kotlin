package gh.marad.chi.truffle.runtime.namespaces;

import com.oracle.truffle.api.CompilerDirectives;
import org.graalvm.collections.EconomicMap;

public class Modules {
    private final EconomicMap<String, Module> modules = EconomicMap.create();

    @CompilerDirectives.TruffleBoundary
    public Module getOrCreateModule(String name) {
        var existingModule = modules.get(name);
        if (existingModule != null) {
            return existingModule;
        } else {
            var createdModule = new Module(name);
            modules.put(name, createdModule);
            return createdModule;
        }
    }

    public void deleteModule(String name) {
        modules.removeKey(name);
    }
}
