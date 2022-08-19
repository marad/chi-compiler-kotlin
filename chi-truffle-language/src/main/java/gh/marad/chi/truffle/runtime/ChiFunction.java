package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.utilities.CyclicAssumption;

@ExportLibrary(InteropLibrary.class)
public class ChiFunction implements ChiValue {
    public static final int INLINE_CACHE_SIZE = 2;
    private RootCallTarget callTarget;
    private String name;
    private final CyclicAssumption callTargetStable;

    public ChiFunction(RootCallTarget callTarget) {
        this.name = callTarget.getRootNode().getName();
        this.callTargetStable = new CyclicAssumption(this.name);
        setCallTarget(callTarget);
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public void setCallTarget(RootCallTarget callTarget) {
        boolean wasNull = this.callTarget == null;
        this.callTarget = callTarget;
        if (!wasNull) {
            callTargetStable.invalidate();
        }
    }

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public boolean hasExecutableName() {
        return true;
    }

    @ExportMessage
    public String getExecutableName() {
        return name;
    }

    @ReportPolymorphism
    @ExportMessage
    abstract static class Execute {
        @Specialization(limit = "INLINE_CACHE_SIZE",
                guards = "function.getCallTarget() == cachedTarget",
                assumptions = "callTargetStable")
        protected static Object doDirect(ChiFunction function, Object[] arguments,
                                         @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                         @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                         @Cached("create(cachedTarget)") DirectCallNode callNode) {
            return callNode.call(arguments);
        }

        @Specialization(replaces = "doDirect")
        protected static Object doIndirect(ChiFunction function, Object[] arguments,
                                           @Cached IndirectCallNode callNode) {
            return callNode.call(function.getCallTarget(), arguments);
        }
    }

    @Override
    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    public Object toDisplayString(boolean allowSideEffects) {
        if (hasExecutableName()) {
            return "<function: %s>".formatted(getExecutableName());
        } else {
            return "<function>";
        }
    }
}
