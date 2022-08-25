package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;
import gh.marad.chi.truffle.runtime.TODO;

public class ReadMemberBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;
    @Child
    private TruffleString.ToJavaStringNode toJavaString;

    public ReadMemberBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
        this.toJavaString = TruffleString.ToJavaStringNode.create();
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getAny(), Type.getAny(), Type.getString());
    }

    @Override
    public String name() {
        return "readMember";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            var receiver = ChiArgs.getArgument(frame, 0);
            var member = (TruffleString) ChiArgs.getArgument(frame, 1);
            return library.readMember(receiver, toJavaString.execute(member));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }
}
