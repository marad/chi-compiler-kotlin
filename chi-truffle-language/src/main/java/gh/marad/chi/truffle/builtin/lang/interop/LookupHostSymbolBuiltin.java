package gh.marad.chi.truffle.builtin.lang.interop;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.core.FnType;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;

public class LookupHostSymbolBuiltin extends LangInteropBuiltin {
    private final TruffleLanguage.Env env;
    @Child
    private TruffleString.ToJavaStringNode toJavaString;

    public LookupHostSymbolBuiltin(TruffleLanguage.Env env) {
        this.env = env;
        this.toJavaString = TruffleString.ToJavaStringNode.create();
    }

    @Override
    public FnType type() {
        return Type.fn(Type.getAny(), Type.getString());
    }

    @Override
    public String name() {
        return "lookupHostSymbol";
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var hostSymbolName = ChiArgs.getTruffleString(frame, 0);
        return env.lookupHostSymbol(toJavaString.execute(hostSymbolName));
    }
}
