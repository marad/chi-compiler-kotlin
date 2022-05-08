package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.LexicalScope;
import gh.marad.chi.truffle.runtime.TODO;

public class ReadVariableExpr extends ChiNode {
    private final String name;
    private final LexicalScope scope;

    public ReadVariableExpr(String name, LexicalScope scope) {
        this.name = name;
        this.scope = scope;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var value = scope.getValue(name);
        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw new TODO("Undefined name");
        }
        return value;
    }
}
