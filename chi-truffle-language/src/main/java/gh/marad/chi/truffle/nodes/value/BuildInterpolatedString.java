package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.TODO;

public class BuildInterpolatedString extends ExpressionNode {

    private final TruffleString.ConcatNode concatNode = TruffleString.ConcatNode.create();

    @Children
    private ChiNode[] parts;

    public BuildInterpolatedString(ChiNode[] parts) {
        this.parts = parts;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        try {
            CompilerAsserts.compilationConstant(parts.length);
            TruffleString acc = parts[0].executeString(frame);
            for (int i = 1; i < parts.length; i++) {
                TruffleString current = parts[i].executeString(frame);
                acc = concatNode.execute(acc, current, TruffleString.Encoding.UTF_8, true);
            }
            return acc;
        } catch (UnexpectedResultException e) {
            throw new TODO("Unexpected value!", e);
        }
    }
}
