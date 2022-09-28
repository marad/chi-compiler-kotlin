package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;
import gh.marad.chi.core.VariantType;

import java.util.Objects;

@ExportLibrary(InteropLibrary.class)
public class ChiObject extends DynamicObject implements ChiValue {
    private final String[] fieldNames;
    private final VariantType type;

    private final TruffleLanguage.Env env;

    public ChiObject(String[] fieldNames, VariantType type, Shape shape, TruffleLanguage.Env env) {
        super(shape);
        this.fieldNames = fieldNames;
        this.type = type;
        this.env = env;
    }

    public VariantType getType() {
        return type;
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
            throw new TODO(e);
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    public Object toDisplayString(boolean allowSideEffects,
                                  @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                                  @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
        var sb = new StringBuilder();
        sb.append(Objects.requireNonNull(type.getVariant()).getVariantName());
        sb.append("(");
        var index = 0;
        for (var key : fieldNames) {
            var value = objectLibrary.getOrDefault(this, key, "");
            sb.append(key);
            sb.append('=');
            sb.append(interopLibrary.toDisplayString(value));
            if (index < fieldNames.length - 1) {
                sb.append(',');
            }
            index += 1;
        }
        sb.append(")");
        return sb.toString();
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doChiObject(ChiObject receiver, ChiObject other,
                                    @CachedLibrary("receiver") DynamicObjectLibrary objectLibrary) {
            var recvShape = objectLibrary.getShape(receiver);
            var otherShape = objectLibrary.getShape(other);
            if (recvShape.equals(otherShape)) {
                var equal = true;
                for (var key : receiver.fieldNames) {
                    var thisField = objectLibrary.getOrDefault(receiver, key, null);
                    var otherField = objectLibrary.getOrDefault(other, key, null);
                    if (receiver.env.isHostObject(thisField) && receiver.env.isHostObject(otherField)) {
                        equal = equal && receiver.env.asHostObject(thisField)
                                                     .equals(receiver.env.asHostObject(otherField));
                    } else {
                        var thisIop = InteropLibrary.getUncached(thisField);
                        var otherIop = InteropLibrary.getUncached(otherField);
                        equal = equal && thisIop.isIdentical(thisField, otherField, otherIop);
                    }
                }
                return equal ? TriState.TRUE : TriState.FALSE;
            } else {
                return TriState.FALSE;
            }
        }

        @Specialization
        static TriState doOther(ChiObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    public int identityHashCode(@CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        var values = new Object[fieldNames.length];
        var i = 0;
        for (var key : fieldNames) {
            values[i++] = objectLibrary.getOrDefault(this, key, null);
        }
        return Objects.hash(values);
    }
}
