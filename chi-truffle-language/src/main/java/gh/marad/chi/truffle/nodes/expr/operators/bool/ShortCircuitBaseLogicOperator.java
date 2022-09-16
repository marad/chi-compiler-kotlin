package gh.marad.chi.truffle.nodes.expr.operators.bool;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.operators.BinaryOperator;
import gh.marad.chi.truffle.runtime.TODO;

public abstract class ShortCircuitBaseLogicOperator extends BinaryOperator {
    @Child
    private ChiNode left;
    @Child
    private ChiNode right;

    public ShortCircuitBaseLogicOperator(ChiNode left, ChiNode right) {
        this.left = left;
        this.right = right;
    }

    private final ConditionProfile evaluateRightProfile = ConditionProfile.createCountingProfile();

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeBoolean(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        try {
            boolean leftValue = left.executeBoolean(frame);
            boolean rightValue;
            if (evaluateRightProfile.profile(shouldEvaluateRight(leftValue))) {
                rightValue = right.executeBoolean(frame);
            } else {
                rightValue = false;
            }
            return execute(leftValue, rightValue);
        } catch (UnexpectedResultException ex) {
            throw new TODO(ex);
        }
    }

    protected abstract boolean shouldEvaluateRight(boolean leftValue);

    protected abstract boolean execute(boolean leftValue, boolean rightValue);
}
