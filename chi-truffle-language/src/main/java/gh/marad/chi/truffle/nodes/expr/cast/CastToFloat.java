package gh.marad.chi.truffle.nodes.expr.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import gh.marad.chi.truffle.nodes.ChiNode;

@NodeChild("value")
public class CastToFloat extends ChiNode {
    @Specialization
    float fromLong(long value) {
        return (float) value;
    }

    @Specialization
    float fromFloat(float value) {
        return value;
    }

    @Specialization
    float fromString(String value) {
        return Float.parseFloat(value);
    }
}
