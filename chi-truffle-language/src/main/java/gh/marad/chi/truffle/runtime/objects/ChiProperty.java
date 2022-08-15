package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.staticobject.StaticProperty;
import gh.marad.chi.truffle.runtime.ChiObject;

public abstract class ChiProperty extends StaticProperty {
    abstract void setGeneric(ChiObject object, Object value);

    abstract Object getGeneric(ChiObject object);

    abstract Class<?> propertyClass();

    public String propertyName() {
        return getId();
    }
}
