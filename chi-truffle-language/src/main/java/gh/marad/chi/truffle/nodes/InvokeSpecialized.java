package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

@NodeChild(value = "function", type = ChiNode.class)
@NodeChild(value = "arguments", type = ChiNode[].class)
public class InvokeSpecialized extends ExpressionNode {
    @Specialization
    public Object invoke(Object function, Object[] arguments,
                         @CachedLibrary(limit = "5") InteropLibrary library) {
        try {
            return library.execute(function, ChiArgs.create(LexicalScope.empty(), arguments));
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }
}
