package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;

public class HasMembersBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;

    public HasMembersBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getBool(), Type.getAny());
    }

    @Override
    public String name() {
        return "hasMembers";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return library.hasMembers(ChiArgs.getObject(frame, 0));
    }
}
