package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;

import java.text.DecimalFormat;

@NodeChild("value")
public class CastToString extends ChiNode {
    private final DecimalFormat df = new DecimalFormat("#.#");
    @Specialization
    String fromLong(long value) {
        return String.format("%d", value);
    }

    @Specialization
    String fromFloat(float value) {
        return df.format(value);
    }

    @Specialization
    String fromString(String value) {
        return value;
    }
}
