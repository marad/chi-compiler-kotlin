package gh.marad.chi.truffle.builtin.lang.interop.members;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.lang.interop.LangInteropBuiltin;

public class HasMemberWriteSideEffectsBuiltin extends LangInteropBuiltin {
    @Child
    private InteropLibrary library;
    @Child
    private TruffleString.ToJavaStringNode toJavaString;

    public HasMemberWriteSideEffectsBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(3);
        this.toJavaString = TruffleString.ToJavaStringNode.create();
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getBool(), Type.getAny(), Type.getString());
    }

    @Override
    public String name() {
        return "hasMemberWriteSideEffects";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var receiver = ChiArgs.getObject(frame, 0);
        var member = ChiArgs.getTruffleString(frame, 1);
        return library.hasMemberWriteSideEffects(receiver, toJavaString.execute(member));
    }
}
