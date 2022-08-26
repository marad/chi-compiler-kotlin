package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class LambdaValue extends ValueNode {
    private final ChiFunction function;
    private LexicalScope scope = null;

    public LambdaValue(ChiFunction function) {
        this.function = function;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        if (scope == null) {
            scope = new LexicalScope(frame.materialize());
            function.bindLexicalScope(scope);
        }
        return function;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeFunction(frame);
    }
}
