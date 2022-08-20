package gh.marad.chi.truffle.builtin.collections;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.builtin.Builtin;
import gh.marad.chi.truffle.runtime.ChiArray;

import java.util.List;

import static gh.marad.chi.core.Type.*;

public class ArrayBuiltin extends Builtin {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var capacity = (Long) ChiArgs.getArgument(frame, 0);
        var defaultValue = ChiArgs.getArgument(frame, 1);
        return new ChiArray(capacity.intValue(), defaultValue);
    }

    @Override
    public Type type() {
        return genericFn(
                List.of(typeParameter("T")),
                array(typeParameter("T")),
                getIntType(),
                typeParameter("T"));
    }

    @Override
    public String getModuleName() {
        return "std";
    }

    @Override
    public String getPackageName() {
        return "collections";
    }

    @Override
    public String name() {
        return "array";
    }
}
