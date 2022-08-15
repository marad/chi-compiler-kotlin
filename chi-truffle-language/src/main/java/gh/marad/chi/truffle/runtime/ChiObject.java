package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
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
}
