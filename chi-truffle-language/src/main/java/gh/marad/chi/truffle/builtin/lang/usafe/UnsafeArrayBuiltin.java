package gh.marad.chi.truffle.builtin.lang.usafe;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.FnType;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.runtime.ChiArray;

import java.util.List;

import static gh.marad.chi.core.Type.*;

public class UnsafeArrayBuiltin extends LangUnsafeBuiltin {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var capacity = (Long) ChiArgs.getArgument(frame, 0);
        return new ChiArray(capacity.intValue());
    }

    @Override
    public FnType type() {
        return genericFn(
                List.of(typeParameter("T")),
                array(typeParameter("T")),
                getIntType());
    }

    @Override
    public String name() {
        return "array";
    }
}
