package gh.marad.chi.truffle;

import gh.marad.chi.core.Atom;
import gh.marad.chi.core.Expression;
import gh.marad.chi.core.Program;
import gh.marad.chi.core.Type;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.nodes.expr.BlockExpr;
import gh.marad.chi.truffle.nodes.value.LongValue;
import gh.marad.chi.truffle.runtime.TODO;

public class Converter {
    public ChiNode convertProgram(Program program) {
        return new BlockExpr(program.getExpressions().stream()
                                    .map(this::convertExpression)
                                    .toList());
    }

    public ChiNode convertExpression(Expression expr) {
        if (expr instanceof Atom atom)  {
            return convertAtom(atom);
        }
        throw new TODO("Unhandled expression conversion: %s".formatted(expr));
    }

    public ChiNode convertAtom(Atom atom) {
        if (atom.getType() == Type.Companion.getI32()) {
            return new LongValue(Long.parseLong(atom.getValue()));
        }
        throw new TODO("Unhandled atom type: %s".formatted(atom.getType()));
    }
}
