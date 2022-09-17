package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import gh.marad.chi.truffle.ChiArgs;

@ExportLibrary(InteropLibrary.class)
public class ChiFunction implements ChiValue {
    public static final int INLINE_CACHE_SIZE = 3;
    private RootCallTarget callTarget;
    private final String name;
    private final CyclicAssumption callTargetStable;
    private LexicalScope boundLexicalScope = null;
    private final CyclicAssumption boundLexicalScopeStable;

    public ChiFunction(RootCallTarget callTarget) {
        this.name = callTarget.getRootNode().getName();
        this.callTargetStable = new CyclicAssumption(this.name);
        this.boundLexicalScopeStable = new CyclicAssumption("bound lexical scope stable");
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

    public Assumption getBoundLexicalScopeStable() {
        return boundLexicalScopeStable.getAssumption();
    }

    public LexicalScope getBoundLexicalScope() {
        return boundLexicalScope;
    }

    public void bindLexicalScope(LexicalScope lexicalScope) {
        boolean wasNull = this.boundLexicalScope == null;
        boundLexicalScope = lexicalScope;
        if (!wasNull) {
            boundLexicalScopeStable.invalidate();
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
                guards = {
                        "function.getCallTarget() == cachedTarget",
                        "function.getBoundLexicalScope() == cachedLexicalScope"
                },
                assumptions = {"callTargetStable", "boundLexicalScopeStable"})
        protected static Object doDirect(ChiFunction function, Object[] arguments,
                                         @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                         @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                         @Cached("function.getBoundLexicalScopeStable()") Assumption boundLexicalScopeStable,
                                         @Cached("function.getBoundLexicalScope()") LexicalScope cachedLexicalScope,
                                         @Cached("create(cachedTarget)") DirectCallNode callNode) {
            return callNode.call(ChiArgs.create(cachedLexicalScope, arguments));
        }

        @Specialization(replaces = "doDirect")
        protected static Object doIndirect(ChiFunction function, Object[] arguments,
                                           @Cached IndirectCallNode callNode) {
            return callNode.call(function.getCallTarget(), ChiArgs.create(function.getBoundLexicalScope(), arguments));
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

    @Override
    @ExportMessage
    public boolean hasLanguage() {
        return ChiValue.super.hasLanguage();
    }

    @Override
    @ExportMessage
    public Class<? extends TruffleLanguage<?>> getLanguage() {
        return ChiValue.super.getLanguage();
    }
}
