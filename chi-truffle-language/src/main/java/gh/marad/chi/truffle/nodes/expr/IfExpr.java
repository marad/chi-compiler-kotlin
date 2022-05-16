package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.operators.bool.LogicNotOperator;
import gh.marad.chi.truffle.runtime.Unit;

public class IfExpr extends ExpressionNode {

    private @Child ChiNode condition;
    private @Child ChiNode thenBranch;
    private @Child ChiNode elseBranch;

    private final ConditionProfile profile = ConditionProfile.createCountingProfile();

    public static IfExpr create(ChiNode condition, ChiNode thenBranch, ChiNode elseBranch) {
        if (condition instanceof LogicNotOperator not) {
            // if (!cond) a else b  => if (cond) b else a
            return new IfExpr(not.getValue(), elseBranch, thenBranch);
        } else {
            return new IfExpr(condition, thenBranch, elseBranch);
        }
    }

    private IfExpr(ChiNode condition, ChiNode thenBranch, ChiNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var cond = condition.executeBoolean(frame);
        if (profile.profile(cond)) {
            if (thenBranch != null) {
                return thenBranch.executeGeneric(frame);
            } else {
                return Unit.instance;
            }
        } else {
            if (elseBranch != null) {
                return elseBranch.executeGeneric(frame);
            } else {
                return Unit.instance;
            }
        }
    }
}
