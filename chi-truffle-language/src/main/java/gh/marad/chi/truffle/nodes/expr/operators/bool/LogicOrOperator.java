package gh.marad.chi.truffle.nodes.expr.operators.bool;

import gh.marad.chi.truffle.nodes.ChiNode;

public class LogicOrOperator extends ShortCircuitBaseLogicOperator {
    public LogicOrOperator(ChiNode left, ChiNode right) {
        super(left, right);
    }

    @Override
    protected boolean shouldEvaluateRight(boolean leftValue) {
        return !leftValue;
    }

    @Override
    protected boolean execute(boolean leftValue, boolean rightValue) {
        return leftValue || rightValue;
    }
}
