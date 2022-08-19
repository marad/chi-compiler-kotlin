package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiStaticObject;

public class ObjectProperty extends ChiProperty {
    private final String name;

    public ObjectProperty(String name) {
        this.name = name;
    }

    @Override
    public void setGeneric(ChiStaticObject object, Object value) {
        setObject(object, value);
    }

    @Override
    public Object getGeneric(ChiStaticObject object) {
        return getObject(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    public Class<?> propertyClass() {
        return ChiStaticObject.class;
    }
}
