package gh.marad.chi.truffle.nodes.expr.flow.effect;

import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.EffectHandlers;
import gh.marad.chi.truffle.nodes.expr.BlockExpr;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

import java.util.Map;

public class HandleEffectNode extends ExpressionNode {

    @Child
    private BlockExpr block;
    private final Map<EffectHandlers.Qualifier, ChiFunction> handlers;

    public HandleEffectNode(BlockExpr resumableBlockNode, Map<EffectHandlers.Qualifier, ChiFunction> handlers) {
        this.block = resumableBlockNode;
        this.handlers = handlers;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            var context = ChiContext.get(this);
            return context.withEffectHandlers(
                    handlers,
                    () -> block.executeGeneric(frame));
        } catch (AbortEffectWithValueException ex) {
            return ex.getValue();
        }
    }
}
