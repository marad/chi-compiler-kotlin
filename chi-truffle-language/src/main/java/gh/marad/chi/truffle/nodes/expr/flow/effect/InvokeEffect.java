package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.EffectHandlers;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.TODO;

public class InvokeEffect extends ExpressionNode {
    private final String moduleName;
    private final String packageName;
    private final String effectName;
    @Child
    private InteropLibrary library;

    public InvokeEffect(String moduleName, String packageName, String effectName) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.effectName = effectName;
        library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var ctx = ChiContext.get(this);
        var function = ctx.findEffectHandlerOrNull(new EffectHandlers.Qualifier(moduleName, packageName, effectName));

        if (function == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new TODO("Invoked effect %s/%s.%s without handler".formatted(moduleName, packageName, effectName));
        }

        try {
            Object[] args = new Object[frame.getArguments().length - 1];
            for (int i = 0; i < frame.getArguments().length - 1; i++) {
                args[i] = frame.getArguments()[i + 1];
            }
            var value = library.execute(function, args);
            throw new AbortEffectWithValueException(value);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            throw new TODO(e);
        } catch (ResumeValueException ex) {
            return ex.getValue();
        }
    }
}
