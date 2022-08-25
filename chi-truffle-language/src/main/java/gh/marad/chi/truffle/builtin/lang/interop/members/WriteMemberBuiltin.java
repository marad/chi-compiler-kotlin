package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;
import gh.marad.chi.truffle.runtime.TODO;

import java.util.List;

public class WriteMemberBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;
    @Child
    private TruffleString.ToJavaStringNode toJavaString;

    public WriteMemberBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
        this.toJavaString = TruffleString.ToJavaStringNode.create();
    }

    @Override
    public FnType type() {
        return Type.genericFn(
                List.of(Type.typeParameter("T")),   // type params
                Type.typeParameter("T"),            // return value
                // receiver, member, value
                Type.getAny(), Type.getString(), Type.typeParameter("T"));
    }

    @Override
    public String name() {
        return "writeMember";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            var receiver = ChiArgs.getArgument(frame, 0);
            var member = (TruffleString) ChiArgs.getArgument(frame, 1);
            var value = ChiArgs.getArgument(frame, 2);
            library.writeMember(receiver, toJavaString.execute(member), value);
            return value;
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }
}
