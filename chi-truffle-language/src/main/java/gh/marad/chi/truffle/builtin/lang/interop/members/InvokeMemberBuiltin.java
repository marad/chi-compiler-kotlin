package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;
import gh.marad.chi.truffle.runtime.TODO;

public class InvokeMemberBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;
    @Child
    private TruffleString.ToJavaStringNode toJavaString;

    public InvokeMemberBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
        this.toJavaString = TruffleString.ToJavaStringNode.create();
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getAny(), Type.getAny(), Type.getString(), Type.array(Type.getAny()));
    }

    @Override
    public String name() {
        return "invokeMember";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            var receiver = ChiArgs.getObject(frame, 0);
            var member = ChiArgs.getTruffleString(frame, 1);
            var argsArray = ChiArgs.getObject(frame, 2);
            var argsArraySize = (int) library.getArraySize(argsArray);
            var args = new Object[argsArraySize];
            for (var i = 0; i < argsArraySize; i++) {
                args[i] = library.readArrayElement(argsArray, i);
            }
            return library.invokeMember(receiver, toJavaString.execute(member), args);
        } catch (UnsupportedMessageException
                 | InvalidArrayIndexException
                 | UnknownIdentifierException
                 | UnsupportedTypeException
                 | ArityException e) {
            throw new TODO(e);
        }
    }
}
