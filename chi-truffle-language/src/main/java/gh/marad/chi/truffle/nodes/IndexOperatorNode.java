package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
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
}