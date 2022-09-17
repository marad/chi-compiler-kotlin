package gh.marad.chi.truffle.nodes.value;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class LambdaValue extends ValueNode {
    private final RootCallTarget callTarget;

    public LambdaValue(RootCallTarget callTarget) {
        this.callTarget = callTarget;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        return generateFunction(frame.materialize());
    }

    @CompilerDirectives.TruffleBoundary
    private ChiFunction generateFunction(MaterializedFrame frame) {
        var function = new ChiFunction(callTarget);
        function.bindLexicalScope(new LexicalScope(frame));
        return function;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeFunction(frame);
    }
}
