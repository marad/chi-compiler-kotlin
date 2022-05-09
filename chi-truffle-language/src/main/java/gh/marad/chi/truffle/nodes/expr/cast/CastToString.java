package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;

@NodeChild("value")
public class CastToString extends ChiNode {
    @Specialization
    String fromLong(long value) {
        return String.format("%d", value);
    }

    @Specialization
    String fromFloat(float value) {
        return String.format("%f", value);
    }

    @Specialization
    String fromString(String value) {
        return value;
    }
}
