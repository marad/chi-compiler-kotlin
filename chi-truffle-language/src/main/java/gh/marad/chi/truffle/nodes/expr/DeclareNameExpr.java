package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class DeclareNameExpr extends ChiNode {
    private final String name;
    private final LexicalScope scope;
    @Child private ChiNode value;
    private int slot;

    public DeclareNameExpr(String name, LexicalScope scope, ChiNode value, int slot) {
        this.name = name;
        this.scope = scope;
        this.value = value;
        this.slot = slot;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var object = value.executeGeneric(frame);
        frame.setObject(slot, object);
//        scope.defineValue(name, object);
        return object;
    }
}
