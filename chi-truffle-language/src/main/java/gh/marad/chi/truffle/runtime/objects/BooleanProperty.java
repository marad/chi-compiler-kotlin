package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiObject;

public class BooleanProperty extends ChiProperty {

    private final String name;

    public BooleanProperty(String name) {
        this.name = name;
    }

    @Override
    void setGeneric(ChiObject object, Object value) {
        setBoolean(object, (boolean) value);
    }

    @Override
    Object getGeneric(ChiObject object) {
        return getBoolean(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    Class<?> propertyClass() {
        return boolean.class;
    }
}
