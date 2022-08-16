package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.Unit;

@NodeChild(value = "receiver", type = ChiNode.class)
@NodeChild(value = "value", type = ChiNode.class)
@NodeField(name = "member", type = String.class)
public abstract class WriteMember extends ExpressionNode {
    protected abstract String getMember();

    @Specialization(rewriteOn = {UnsupportedMessageException.class, UnknownIdentifierException.class, UnsupportedTypeException.class})
    public Object writeMember(Object receiver, Object value,
                              @CachedLibrary(limit = "3") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        interop.writeMember(receiver, getMember(), value);
        return Unit.instance;
    }
}
