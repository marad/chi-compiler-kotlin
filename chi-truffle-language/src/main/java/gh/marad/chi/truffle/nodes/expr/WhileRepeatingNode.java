package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.TODO;

public class WhileRepeatingNode extends ExpressionNode implements RepeatingNode {

    @Child
    private ChiNode conditionNode;
    @Child
    private ChiNode bodyNode;

    private final BranchProfile continueTaken = BranchProfile.create();
    private final BranchProfile breakTaken = BranchProfile.create();

    public WhileRepeatingNode(ChiNode conditionNode, ChiNode bodyNode) {
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        try {
            if (!conditionNode.executeBoolean(frame)) {
                return false;
            }
            bodyNode.executeVoid(frame);
            return true;
        } catch (UnexpectedResultException ex) {
            throw new TODO(ex);
        } catch (BreakException ex) {
            breakTaken.enter();
            return false;
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new TODO("while is not a regular expression node");
    }

    //    @Override
//    public Object executeRepeatingWithValue(VirtualFrame frame) {
//        return RepeatingNode.super.executeRepeatingWithValue(frame);
//    }
//
//    @Override
//    public Object initialLoopStatus() {
//        return RepeatingNode.super.initialLoopStatus();
//    }
//
//    @Override
//    public boolean shouldContinue(Object returnValue) {
//        return RepeatingNode.super.shouldContinue(returnValue);
//    }
}
