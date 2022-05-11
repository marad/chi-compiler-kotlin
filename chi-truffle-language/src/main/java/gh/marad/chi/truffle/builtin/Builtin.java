package gh.marad.chi.truffle.builtin;

import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.nodes.ChiNode;

public abstract class Builtin extends ChiNode {
    public abstract Type type();
    public abstract String name();
}
