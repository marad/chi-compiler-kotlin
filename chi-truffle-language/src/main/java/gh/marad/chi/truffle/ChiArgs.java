package gh.marad.chi.truffle;


import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.strings.TruffleString;
import gh.marad.chi.truffle.runtime.ChiArray;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class ChiArgs {
    private final static int SCOPE_INDEX = 0;
    public final static int ARGS_OFFSET = SCOPE_INDEX + 1;

    public static boolean isChiArgs(Object[] args) {
        return args.length > 0 && args[SCOPE_INDEX] instanceof LexicalScope;
    }

    public static Object[] create(LexicalScope scope) {
        return create(scope, new Object[0]);
    }

    public static Object[] create(Object[] args) {
        return create(LexicalScope.empty(), args);
    }

    public static Object[] create(LexicalScope scope, Object[] args) {
        var result = new Object[args.length + ARGS_OFFSET];
        result[SCOPE_INDEX] = scope;
        System.arraycopy(args, 0, result, ARGS_OFFSET, args.length);
        return result;
    }

    public static LexicalScope getParentScope(Frame frame) {
        return (LexicalScope) frame.getArguments()[SCOPE_INDEX];
    }

    public static Object getObject(Frame frame, int argumentIndex) {
        return frame.getArguments()[ARGS_OFFSET + argumentIndex];
    }

    public static long getLong(Frame frame, int argumentIndex) {
        return ChiTypesGen.asImplicitLong(getObject(frame, argumentIndex));
    }

    public static TruffleString getTruffleString(Frame frame, int argumentIndex) {
        return ChiTypesGen.asImplicitTruffleString(getObject(frame, argumentIndex));
    }

    public static float getFloat(Frame frame, int argumentIndex) {
        return ChiTypesGen.asImplicitFloat(getObject(frame, argumentIndex));
    }

    public static boolean getBoolean(Frame frame, int argumentIndex) {
        return ChiTypesGen.asBoolean(getObject(frame, argumentIndex));
    }

    public static ChiArray getChiArray(Frame frame, int argumentIndex) {
        return ChiTypesGen.asImplicitChiArray(getObject(frame, argumentIndex));
    }

    public static void setArgument(Frame frame, int argumentIndex, Object value) {
        frame.getArguments()[ARGS_OFFSET + argumentIndex] = value;
    }
}
