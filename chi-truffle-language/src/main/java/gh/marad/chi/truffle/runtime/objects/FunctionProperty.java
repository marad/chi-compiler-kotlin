package gh.marad.chi.truffle.runtime.objects;

import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.ChiObject;

public class FunctionProperty extends ChiProperty {

    private final String name;

    public FunctionProperty(String name) {
        this.name = name;
    }

    @Override
    public void setGeneric(ChiObject object, Object value) {
        setObject(object, value);
    }

    @Override
    public Object getGeneric(ChiObject object) {
        return getObject(object);
    }

    @Override
    protected String getId() {
        return name;
    }

    @Override
    public Class<?> propertyClass() {
        return ChiFunction.class;
    }
}
