package gh.marad.chi.truffle.builtin.collections;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import gh.marad.chi.core.FnType;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.runtime.TODO;

import java.util.List;

import static gh.marad.chi.core.Type.*;

public class SizeBuiltin extends CollectionsArrayBuiltin {
    @Child
    private InteropLibrary library;

    public SizeBuiltin() {
        this.library = InteropLibrary.getFactory().createDispatched(5);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var object = ChiArgs.getArgument(frame, 0);
        try {
            return library.getArraySize(object);
        } catch (UnsupportedMessageException e) {
            throw new TODO(e);
        }
    }

    @Override
    public FnType type() {
        return genericFn(
                List.of(typeParameter("T")),
                getIntType(),
                array(typeParameter("T")));
    }

    @Override
    public String name() {
        return "size";
    }
}
