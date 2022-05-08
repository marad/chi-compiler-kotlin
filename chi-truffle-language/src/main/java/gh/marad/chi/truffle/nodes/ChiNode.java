package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.runtime.TODO;

public abstract class ChiNode extends Node {
//    public String executeString(VirtualFrame frame) {
//        var value = this.executeGeneric(frame);
//        try {
//            return ChiTypesGen.expectString(value);
//        } catch (UnexpectedResultException ex) {
//            CompilerDirectives.transferToInterpreter();
//            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
//        }
//    }
    public long executeLong(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectLong(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
        }
    }
//
//    public boolean executeBoolean(VirtualFrame frame) {
//        var value = this.executeGeneric(frame);
//        try {
//            return ChiTypesGen.expectBoolean(value);
//        } catch (UnexpectedResultException ex) {
//            CompilerDirectives.transferToInterpreter();
//            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
//        }
//    }
//
//    public ChiFn executeFunction(VirtualFrame frame) {
//        var value = this.executeGeneric(frame);
//        try {
//            return ChiTypesGen.expectChiFn(value);
//        } catch (UnexpectedResultException ex) {
//            CompilerDirectives.transferToInterpreter();
//            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
//        }
//    }

    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    public Object executeGeneric(VirtualFrame frame) {
        throw new TODO();
    }
}
