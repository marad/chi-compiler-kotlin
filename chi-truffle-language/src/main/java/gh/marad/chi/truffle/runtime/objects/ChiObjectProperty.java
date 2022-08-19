package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.staticobject.StaticProperty;

public class ChiObjectProperty extends StaticProperty {
    private final String name;
    private final Class<?> clazz;

    public ChiObjectProperty(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @Override
    protected String getId() {
        return name;
    }

    public Class<?> getPropertyClass() {
        return clazz;
    }
}
