package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiObject;

public class FloatProperty extends ChiProperty {

    private final String name;

    public FloatProperty(String name) {
        this.name = name;
    }

    @Override
    public void setGeneric(ChiObject object, Object value) {
        setFloat(object, (float) value);
    }

    @Override
    public Object getGeneric(ChiObject object) {
        return getFloat(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    public Class<?> propertyClass() {
        return float.class;
    }
}
