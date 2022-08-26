package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

@ExportLibrary(InteropLibrary.class)
public class ChiObject extends DynamicObject implements ChiValue {
    private final String simpleTypeName;
    private final String[] fieldNames;

    public ChiObject(String simpleTypeName, String[] fieldNames, Shape shape) {
        super(shape);
        this.simpleTypeName = simpleTypeName;
        this.fieldNames = fieldNames;
    }

    @ExportMessage
    boolean hasMembers() {
        return getShape().getPropertyCount() > 0;
    }

    @ExportMessage
    Object readMember(String name,
                      @CachedLibrary("this") DynamicObjectLibrary objectLibrary) throws UnknownIdentifierException {
        Object result = objectLibrary.getOrDefault(this, name, null);
        if (result == null) {
            throw UnknownIdentifierException.create(name);
        }
        return result;
    }

    @ExportMessage
    void writeMember(String name, Object value,
                     @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        objectLibrary.put(this, name, value);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return objectLibrary.containsKey(this, member);
    }

    @ExportMessage
    Object getMembers(boolean includeInternal,
                      @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return new ChiArray(objectLibrary.getKeyArray(this));
    }

    @ExportMessage
    boolean isMemberModifiable(String member) {
        return getShape().hasProperty(member);
    }

    @ExportMessage
    boolean isMemberInsertable(String member) {
        return !getShape().hasProperty(member);
    }

    @ExportMessage
    boolean isMemberInvocable(String member,
                              @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                              @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            return isMemberReadable(member, objectLibrary)
                           && interop.isExecutable(readMember(member, objectLibrary));
        } catch (UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }

    @ExportMessage
    public Object invokeMember(String member,
                               Object[] arguments,
                               @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                               @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            return interop.execute(readMember(member, objectLibrary), arguments);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException |
                 UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    public Object toDisplayString(boolean allowSideEffects,
                                  @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        var sb = new StringBuilder();
        sb.append(simpleTypeName);
        sb.append("(");
        var index = 0;
        for (var key : fieldNames) {
            sb.append(key);
            sb.append('=');
            sb.append(objectLibrary.getOrDefault(this, key, ""));
            if (index < fieldNames.length - 1) {
                sb.append(',');
            }
            index += 1;
        }
        sb.append(")");
        return sb.toString();
    }
}
