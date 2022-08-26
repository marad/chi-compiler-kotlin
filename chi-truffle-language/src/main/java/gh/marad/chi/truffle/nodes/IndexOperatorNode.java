package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiArray;
import gh.marad.chi.truffle.runtime.TODO;

@NodeChild(value = "variable", type = ChiNode.class)
@NodeChild(value = "index", type = ChiNode.class)
public class IndexOperatorNode extends ExpressionNode {
    @Specialization
    public Object doChiArray(ChiArray array, long index) {
        try {
            return array.readArrayElement(index);
        } catch (InvalidArrayIndexException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO("Implement runtime error handling!");
        }
    }

    @Specialization
    public TruffleString doString(TruffleString string, long index,
                                  @Cached TruffleString.CodePointAtByteIndexNode node) {
        int codePoint = node.execute(string, (int) index, TruffleString.Encoding.UTF_8);
        return TruffleString.fromCodePointUncached(codePoint, TruffleString.Encoding.UTF_8);
    }

    @Specialization(replaces = {"doChiArray", "doString"})
    public Object doObject(Object indexable, long index,
                           @CachedLibrary(limit = "3") InteropLibrary library) {
        try {
            return library.readArrayElement(indexable, index);
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO(e);
        }
    }
}
