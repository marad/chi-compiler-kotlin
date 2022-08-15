package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiObject;

public class ObjectProperty extends ChiProperty {
    private final String name;

    public ObjectProperty(String name) {
        this.name = name;
    }

    @Override
    void setGeneric(ChiObject object, Object value) {
        setObject(object, value);
    }

    @Override
    Object getGeneric(ChiObject object) {
        return getObject(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    Class<?> propertyClass() {
        return ChiObject.class;
    }
}
