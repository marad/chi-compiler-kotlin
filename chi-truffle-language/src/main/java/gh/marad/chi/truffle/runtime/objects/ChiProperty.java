package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.staticobject.StaticProperty;
import gh.marad.chi.truffle.runtime.ChiStaticObject;

public abstract class ChiProperty extends StaticProperty {
    public abstract void setGeneric(ChiStaticObject object, Object value);

    public abstract Object getGeneric(ChiStaticObject object);

    public abstract Class<?> propertyClass();

    public String propertyName() {
        return getId();
    }
}
