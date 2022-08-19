package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;

@ExportLibrary(InteropLibrary.class)
public class ChiObject extends ChiValue {
    private final ChiObjectDescriptor descriptor;

    public ChiObject(ChiObjectDescriptor descriptor1) {
        this.descriptor = descriptor1;
    }

    @Override
    @ExportMessage
    public Object toDisplayString(boolean allowSideEffects) {
        return descriptor.asString(this);
    }

    @ExportMessage
    public boolean hasMembers() {
        return !descriptor.isSingleValueType();
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        return new ChiArray(descriptor.getPropertyNames());
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return true;
    }

    @ExportMessage
    public boolean isMemberModifiable(String member) {
        return true;
    }

    @ExportMessage
    public boolean isMemberInsertable(String member) {
        return false;
    }

    @ExportMessage
    public Object readMember(String member) {
        var property = descriptor.getProperty(member);
        return property.getGeneric(this);
    }

    @ExportMessage
    public void writeMember(String member, Object value) {
        var property = descriptor.getProperty(member);
        property.setGeneric(this, value);
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        var property = descriptor.getProperty(member);
        return property.propertyClass() == ChiFunction.class;
    }

    @ExportMessage
    public Object invokeMember(String member, Object... arguments) throws UnsupportedMessageException, UnsupportedTypeException, ArityException {
        var fn = readMember(member);
        return InteropLibrary.getUncached().execute(fn, arguments);
    }
}
