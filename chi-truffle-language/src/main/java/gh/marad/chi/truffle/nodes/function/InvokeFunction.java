package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

import java.util.Collection;

public class InvokeFunction extends ExpressionNode {
    @Child ChiNode function;
    @Children ChiNode[] arguments;
    private final InteropLibrary library;

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
        var args = new Object[arguments.length];
        for(var i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].executeGeneric(frame);
        }

        try {
            return fn.execute(args);
//            return library.execute(fn, args);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        } finally {
            TruffleSafepoint.poll(this);
        }

    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.CallTag.class || super.hasTag(tag);
    }
}