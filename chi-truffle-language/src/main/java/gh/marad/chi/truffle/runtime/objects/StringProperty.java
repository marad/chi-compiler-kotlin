package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiObject;

public class StringProperty extends ChiProperty {

    private final String name;

    public StringProperty(String name) {
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
        return TruffleString.class;
    }
}
