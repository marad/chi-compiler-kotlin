package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiObject;

public class StringProperty extends ChiProperty {

    private final String name;

    public StringProperty(String name) {
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
        return TruffleString.class;
    }
}
