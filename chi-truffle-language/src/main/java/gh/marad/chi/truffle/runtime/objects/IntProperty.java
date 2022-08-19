package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiStaticObject;

public class IntProperty extends ChiProperty {
    private final String name;

    public IntProperty(String name) {
        this.name = name;
    }

    @Override
    public void setGeneric(ChiStaticObject object, Object value) {
        setLong(object, (long) value);
    }

    @Override
    public Object getGeneric(ChiStaticObject object) {
        return getLong(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    public Class<?> propertyClass() {
        return long.class;
    }
}
