package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.ChiTypes;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.runtime.ChiFunction;
import gh.marad.chi.truffle.runtime.TODO;

@Introspectable
@TypeSystemReference(ChiTypes.class)
@NodeInfo(language = ChiLanguage.id, description = "Base for all Chi nodes.")
public abstract class ChiNode extends Node {
    public long executeLong(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectLong(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
        }
    }

    public float executeFloat(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectFloat(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
        }
    }

    public boolean executeBoolean(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectBoolean(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
        }
    }


    public String executeString(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectString(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedSpecializationException(this, new Node[0], ex.getResult());
        }
    }

    public ChiFunction executeFunction(VirtualFrame frame) {
        var value = this.executeGeneric(frame);
        try {
            return ChiTypesGen.expectChiFunction(value);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreter();
//            throw new UnsupportedSpecializationException(this, new Node[1], ex.getResult());
            throw new RuntimeException("Unexpected result for function: " + ex.getResult());
        }
    }

    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    public Object executeGeneric(VirtualFrame frame) {
        throw new TODO();
    }
}
