package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public class ConstructDynamicObject extends ExpressionNode {
    private final ChiLanguage language;
    private final String[] fieldNames;
    private final InteropLibrary interopLibrary;

    public ConstructDynamicObject(ChiLanguage language, String[] fieldNames) {
        this.language = language;
        this.fieldNames = fieldNames;
        interopLibrary = InteropLibrary.getUncached();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var object = language.createDynamicObject();
        for (int i = 0; i < fieldNames.length; i++) {
            try {
                interopLibrary.writeMember(object, fieldNames[i], ChiArgs.getArgument(frame, i));
            } catch (UnsupportedMessageException | UnsupportedTypeException | UnknownIdentifierException e) {
                throw new RuntimeException(e);
            }
        }
        return object;
    }
}
