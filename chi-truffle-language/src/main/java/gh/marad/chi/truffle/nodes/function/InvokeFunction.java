package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

import java.util.*;

public class InvokeFunction extends ExpressionNode {
    @Child private ChiNode function;
    @Children private final ChiNode[] arguments;
    @Child private InteropLibrary library;


    public InvokeFunction(ChiNode function, Collection<ChiNode> arguments) {
        this.function = function;
        this.arguments = arguments.toArray(new ChiNode[0]);
        library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        var fn = function.executeFunction(frame);

        CompilerAsserts.compilationConstant(arguments.length);
        Object[] args = new Object[arguments.length+1];
        args[0] = new FunctionScope(frame.materialize());
        for(int i = 1; i <= arguments.length; i++) {
            args[i] = arguments[i-1].executeGeneric(frame);
        }

        try {
            return library.execute(fn, args);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.CallTag.class || super.hasTag(tag);
    }
}
