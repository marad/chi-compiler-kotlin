package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;

@ExportLibrary(InteropLibrary.class)
public class ChiFunction implements TruffleObject {
    private final RootCallTarget callTarget;
    private final DirectCallNode callNode;

    public ChiFunction(RootCallTarget callTarget) {
        this.callTarget = callTarget;
        this.callNode = Truffle.getRuntime().createDirectCallNode(callTarget);
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public DirectCallNode getCallNode() {
        return callNode;
    }

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public boolean hasExecutableName() { return true; }

    @ExportMessage
    public String getExecutableName() { return "<anonymousFn>"; }

    @ExportMessage
    public Object execute(Object [] arguments) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return callNode.call(arguments);
    }
}
