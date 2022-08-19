package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.staticobject.StaticProperty;
import gh.marad.chi.truffle.runtime.ChiObject;

public abstract class ChiProperty extends StaticProperty {
    public abstract void setGeneric(ChiObject object, Object value);

    public abstract Object getGeneric(ChiObject object);

    public abstract Class<?> propertyClass();

    public String propertyName() {
        return getId();
    }
}
