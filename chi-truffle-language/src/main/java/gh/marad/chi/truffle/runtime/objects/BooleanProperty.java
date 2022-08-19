package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiStaticObject;

public class BooleanProperty extends ChiProperty {

    private final String name;

    public BooleanProperty(String name) {
        this.name = name;
    }

    @Override
    public void setGeneric(ChiStaticObject object, Object value) {
        setBoolean(object, (boolean) value);
    }

    @Override
    public Object getGeneric(ChiStaticObject object) {
        return getBoolean(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    public Class<?> propertyClass() {
        return boolean.class;
    }
}
