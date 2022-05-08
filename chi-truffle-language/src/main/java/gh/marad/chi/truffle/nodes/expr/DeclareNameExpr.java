package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class DeclareNameExpr extends ChiNode {
    private final String name;
    private final LexicalScope scope;
    @Child private ChiNode value;

    public DeclareNameExpr(String name, LexicalScope scope, ChiNode value) {
        this.name = name;
        this.scope = scope;
        this.value = value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var object = value.executeGeneric(frame);
        scope.defineValue(name, object);
        return object;
    }
}
