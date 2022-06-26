package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ReadOuterScope extends ExpressionNode {
    private final String name;

    public ReadOuterScope(String name) {
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var currentScope = getParentScope(frame);
        Object result;

        while(true) {
            result = currentScope.getValue(name);
            if (result != null) {
                return result;
            }
            currentScope = currentScope.getParentScope();
            if (currentScope == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("Variable %s cannot be found in the outer scopes".formatted(name));
            }
        }

    }

}
