package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiObject;

public class IntProperty extends ChiProperty {
    private final String name;

    public IntProperty(String name) {
        this.name = name;
    }

    @Override
    void setGeneric(ChiObject object, Object value) {
        setLong(object, (long) value);
    }

    @Override
    Object getGeneric(ChiObject object) {
        return getLong(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    Class<?> propertyClass() {
        return long.class;
    }
}
