package gh.marad.chi.truffle.nodes.expr.operators;

import com.oracle.truffle.api.dsl.Fallback;

public abstract class BinaryOperatorWithFallback extends BinaryOperator {
    @Fallback
    public void fallback(Object left, Object right) {
        throw new RuntimeException("Unexpected %s and %s for operator".formatted(left, right));
    }
}
