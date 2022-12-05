package gh.marad.chi.truffle.builtin.lang.interop.values;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;

public class IsNullBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;

    public IsNullBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getBool(), Type.getAny());
    }

    @Override
    public String name() {
        return "isNull";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var arg = ChiArgs.getObject(frame, 0);
        return library.isNull(arg);
    }
}
