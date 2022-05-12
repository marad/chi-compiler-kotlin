package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class AssignmentExpr extends ChiNode {
    private final String name;
    private final ChiNode valueNode;
    private final LexicalScope scope;

    public AssignmentExpr(String name, ChiNode valueNode, LexicalScope scope) {
        this.name = name;
        this.valueNode = valueNode;
        this.scope = scope;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var value = valueNode.executeGeneric(frame);
        scope.setValue(name, value);
        return value;
    }
}
