package gh.marad.chi.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import gh.marad.chi.core.VariantType;
import gh.marad.chi.truffle.ChiArgs;
import gh.marad.chi.truffle.ChiContext;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

import java.util.Objects;

public class ConstructChiObject extends ExpressionNode {
    private final ChiLanguage language;
    private final String[] fieldNames;
    private final InteropLibrary interopLibrary;
    private final VariantType type;

    public ConstructChiObject(ChiLanguage language, VariantType type) {
        this.language = language;
        this.fieldNames = Objects.requireNonNull(type.getVariant()).getFields().stream()
                                 .map(VariantType.VariantField::getName).toList().toArray(new String[0]);
        this.type = type;
        interopLibrary = InteropLibrary.getUncached();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var env = ChiContext.get(this).getEnv();
        var object = language.createObject(fieldNames, type, env);
        for (int i = 0; i < fieldNames.length; i++) {
            try {
                interopLibrary.writeMember(object, fieldNames[i], ChiArgs.getArgument(frame, i));
            } catch (UnsupportedMessageException | UnsupportedTypeException | UnknownIdentifierException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(e);
            }
        }
        return object;
    }
}
