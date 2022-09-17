package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;
import gh.marad.chi.truffle.runtime.TODO;

public class GetMembersBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;

    public GetMembersBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    public FnType type() {
        return Type.fn(Type.array(Type.getString()), Type.getAny(), Type.getBool());
    }

    @Override
    public String name() {
        return "getMembers";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            var receiver = ChiArgs.getArgument(frame, 0);
            var includeInternal = (Boolean) ChiArgs.getArgument(frame, 1);
            return library.getMembers(receiver, includeInternal);
        } catch (UnsupportedMessageException e) {
            throw new TODO(e);
        }
    }
}
