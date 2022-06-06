package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class DeclareNameExpr extends ExpressionNode {
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

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.WriteVariableTag.class || super.hasTag(tag);
    }
}
