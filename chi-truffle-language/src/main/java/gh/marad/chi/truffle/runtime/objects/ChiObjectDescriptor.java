package gh.marad.chi.truffle.runtime.objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.staticobject.StaticShape;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.runtime.ChiObject;
import gh.marad.chi.truffle.runtime.ChiObjectFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChiObjectDescriptor {
    private final ChiLanguage language;
    private final String typeName;
    private final ChiProperty[] properties;
    private final ChiObjectFactory objectFactory;
    private final InteropLibrary interopLibrary;

    public ChiObjectDescriptor(ChiLanguage language, String typeName, List<ChiProperty> properties) {
        this.language = language;
        this.typeName = typeName;
        this.properties = properties.toArray(new ChiProperty[0]);
        objectFactory = createObjectFactory();
        interopLibrary = InteropLibrary.getUncached();
    }

    public String getTypeName() {
        return typeName;
    }

    @ExplodeLoop
    private ChiObjectFactory createObjectFactory() {
        CompilerAsserts.compilationConstant(properties.length);
        var builder = StaticShape.newBuilder(language);
        for (var property : properties) {
            builder.property(property, property.propertyClass(), false);
        }
        var shape = builder.build(ChiObject.class, ChiObjectFactory.class);
        return shape.getFactory();
    }

    @ExplodeLoop
    public ChiObject constructObject(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(properties.length);
        var object = objectFactory.create(this);
        var index = 0;
        for (var property : properties) {
            property.setGeneric(object, ChiArgs.getArgument(frame, index++));
        }
        return object;
    }

    public boolean isSingleValueType() {
        return properties.length == 0;
    }

    public @Nullable ChiProperty getProperty(String name) {
        for (var property : properties) {
            if (property.propertyName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @ExplodeLoop
    public String[] getPropertyNames() {
        CompilerAsserts.compilationConstant(properties.length);
        var names = new String[properties.length];
        for (var index = 0; index < properties.length; index++) {
            names[index] = properties[index].propertyName();
        }
        return names;
    }

    @CompilerDirectives.TruffleBoundary
    @ExplodeLoop
    public String asString(ChiObject object) {
        CompilerAsserts.compilationConstant(properties.length);
        var sb = new StringBuilder();
        sb.append(typeName);
        if (properties.length > 0) {
            sb.append('(');
            var index = 0;
            for (var property : properties) {
                var value = property.getGeneric(object);
                sb.append(property.propertyName());
                sb.append('=');
                sb.append(interopLibrary.toDisplayString(value));
                if (++index < properties.length) {
                    sb.append(',');
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
