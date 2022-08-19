package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

@NodeChild(value = "receiver", type = ChiNode.class)
@NodeField(name = "member", type = String.class)
public abstract class ReadMember extends ExpressionNode {
    protected abstract String getMember();

    @Specialization(rewriteOn = {UnsupportedMessageException.class, UnknownIdentifierException.class})
    public Object readMember(Object receiver,
                             @CachedLibrary(limit = "3") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException {
        return interop.readMember(receiver, getMember());
    }
}
