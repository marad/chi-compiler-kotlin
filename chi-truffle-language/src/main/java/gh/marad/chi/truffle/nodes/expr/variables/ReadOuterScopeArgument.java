package gh.marad.chi.truffle.nodes.expr.variables;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ReadOuterScopeArgument extends ExpressionNode {
    private final int scopesUp;
    private final int argIndex;

    public ReadOuterScopeArgument(int scopesUp, int argIndex) {
        this.scopesUp = scopesUp;
        this.argIndex = argIndex;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var currentScope = getParentScope(frame);
        Object result;

        var scopesLeft = scopesUp - 1;
        while (scopesLeft > 0) {
            currentScope = currentScope.getParentScope();
            currentScope = currentScope.getParentScope();
            if (currentScope == null) {
                break;
            }
        }
        if (currentScope == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Argument cannot be found in the outer scopes");
        }
        return currentScope.getArgument(argIndex);
    }

}
