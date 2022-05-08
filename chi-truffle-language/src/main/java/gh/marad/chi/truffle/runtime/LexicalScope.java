package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.profiles.ConditionProfile;

import java.util.HashMap;
import java.util.Map;

public class LexicalScope {
    private final LexicalScope parent;
    private final Map<String, Object> objects = new HashMap<>();
    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public LexicalScope() {
        this.parent = null;
    }

    public LexicalScope(LexicalScope parent) {
        this.parent = parent;
    }

    public Object getValue(String name) {
        var localValue = objects.get(name);
        if (localValue == null && parent != null) {
            return  parent.getValue(name);
        }
        return localValue;
    }

    public void defineValue(String name, Object value) {
        objects.put(name, value);
    }

    public void setValue(String name, Object value) {
        if (conditionProfile.profile(objects.containsKey(name))) {
            objects.put(name, value);
        } else if (parent != null) {
            parent.setValue(name, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new TODO("Should not happen. Signals compilation problem.");
        }
    }
}
