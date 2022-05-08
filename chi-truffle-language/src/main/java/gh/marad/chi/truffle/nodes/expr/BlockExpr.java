package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import gh.marad.chi.truffle.nodes.ChiNode;

import java.util.Collection;

public class BlockExpr extends ChiNode  {
    @Children final ChiNode[] body;

    public BlockExpr(Collection<ChiNode> body) {
        this.body = body.toArray(new ChiNode[]{});
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(body.length);
        for (int i=0; i < body.length-1; i++) {
            body[i].executeVoid(frame);
        }
        return body[body.length-1].executeGeneric(frame);
    }
}
