package gh.marad.chi.truffle.nodes;

import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.ChiTypes;
import gh.marad.chi.truffle.ChiTypesGen;
import gh.marad.chi.truffle.runtime.ChiFunction;

@Introspectable
@TypeSystemReference(ChiTypes.class)
@NodeInfo(language = ChiLanguage.id, description = "Base for all Chi nodes.")
public abstract class ChiNode extends Node {
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        var value = this.executeGeneric(frame);
        return ChiTypesGen.expectLong(value);
    }

    public float executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        var value = this.executeGeneric(frame);
        return ChiTypesGen.expectFloat(value);
    }

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        var value = this.executeGeneric(frame);
        return ChiTypesGen.expectBoolean(value);
    }


    public TruffleString executeString(VirtualFrame frame) throws UnexpectedResultException {
        var value = this.executeGeneric(frame);
        return ChiTypesGen.expectTruffleString(value);
    }

    public ChiFunction executeFunction(VirtualFrame frame) throws UnexpectedResultException {
        var value = this.executeGeneric(frame);
        return ChiTypesGen.expectChiFunction(value);
    }

    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    public abstract Object executeGeneric(VirtualFrame frame);
}
