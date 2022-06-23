package gh.marad.chi.truffle;


import com.oracle.truffle.api.frame.Frame;
import gh.marad.chi.truffle.runtime.LexicalScope;

public class ChiArgs {
    private final static int SCOPE_INDEX = 0;
    private final static int ARGS_OFFSET = SCOPE_INDEX + 1;

    public static Object[] create(LexicalScope scope, Object[] args) {
        var result = new Object[args.length + ARGS_OFFSET];
        result[SCOPE_INDEX] = scope;
        System.arraycopy(args, 0, result, ARGS_OFFSET, args.length);
        return result;
    }

    public static LexicalScope getParentScope(Frame frame) {
        return (LexicalScope) frame.getArguments()[SCOPE_INDEX];
    }

    public static Object getArgument(Frame frame, int argumentIndex) {
        return frame.getArguments()[ARGS_OFFSET + argumentIndex];
    }
}
