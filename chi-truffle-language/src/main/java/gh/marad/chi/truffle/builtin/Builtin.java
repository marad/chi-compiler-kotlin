package gh.marad.chi.truffle.builtin;

import gh.marad.chi.core.FnType;
import gh.marad.chi.truffle.nodes.expr.ExpressionNode;

public abstract class Builtin extends ExpressionNode {
    public abstract FnType type();

    public abstract String getModuleName();

    public abstract String getPackageName();

    public abstract String name();
}
